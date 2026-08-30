import { useState } from "react";
import { useLanguage } from "../i18n/LanguageContext.jsx";

const notificationTypeTranslationKeys = {
  SCHEDULE_PUBLISHED: "schedulePublished",
  REQUEST_CREATED: "requestCreatedNotification",
};

function NotificationCenter({
  formatDateTime,
  isLoadingNotifications,
  markingNotificationId,
  notifications,
  notificationsError,
  onMarkNotificationRead,
  onOpenRelatedEntity,
  onRefreshNotifications,
  unreadNotificationCount,
}) {
  const { t } = useLanguage();
  const [isOpen, setIsOpen] = useState(false);

  function notificationTypeLabel(notification) {
    return t(notificationTypeTranslationKeys[notification.type] ?? notification.type);
  }

  function notificationTitle(notification) {
    return notification.type === "SCHEDULE_PUBLISHED"
      ? t("schedulePublished")
      : notification.title;
  }

  function handleOpenRelatedEntity(notification) {
    onOpenRelatedEntity(notification);
    setIsOpen(false);
  }

  return (
    <div className="notification-center" id="notifications">
      <button
        aria-controls="notification-panel"
        aria-expanded={isOpen}
        aria-label={isOpen ? t("closeNotifications") : t("openNotifications")}
        className="notification-trigger"
        onClick={() => setIsOpen((current) => !current)}
        type="button"
      >
        <span>{t("notifications")}</span>
        {unreadNotificationCount > 0 ? <span className="nav-count">{unreadNotificationCount}</span> : null}
      </button>

      {isOpen ? (
        <div aria-label={t("notificationCenter")} className="notification-panel" id="notification-panel">
          <div className="notification-panel-heading">
            <div>
              <h3>{t("notifications")}</h3>
              <p>
                {unreadNotificationCount} {t("unread")}
              </p>
            </div>
            <button
              className="secondary-button compact-button"
              disabled={isLoadingNotifications}
              onClick={onRefreshNotifications}
              type="button"
            >
              {t("refresh")}
            </button>
          </div>

          {isLoadingNotifications ? <p className="muted">{t("loadingNotifications")}</p> : null}
          {notificationsError ? <p className="error-message">{notificationsError}</p> : null}

          {!isLoadingNotifications && !notificationsError && notifications.length === 0 ? (
            <p className="muted">{t("noNotifications")}</p>
          ) : null}

          {notifications.length > 0 ? (
            <div className="notification-list">
              {notifications.map((notification) => (
                <article
                  className={notification.read ? "notification-row" : "notification-row unread-notification"}
                  key={notification.id}
                >
                  <div>
                    <div className="notification-title-row">
                      <h3>{notificationTitle(notification)}</h3>
                      {!notification.read ? <span>{t("unread")}</span> : null}
                    </div>
                    <p>{notification.message}</p>
                    <p className="notification-meta">
                      {notificationTypeLabel(notification)} - {formatDateTime(notification.createdAt)}
                    </p>
                  </div>

                  <div className="notification-card-actions">
                    {notification.relatedEntityType === "SCHEDULE" && notification.relatedEntityId ? (
                      <button
                        className="secondary-button compact-button"
                        onClick={() => handleOpenRelatedEntity(notification)}
                        type="button"
                      >
                        {t("viewPublishedSchedule")}
                      </button>
                    ) : null}
                    {notification.relatedEntityType === "REQUEST" && notification.relatedEntityId ? (
                      <button
                        className="secondary-button compact-button"
                        onClick={() => handleOpenRelatedEntity(notification)}
                        type="button"
                      >
                        {t("viewTransferRequests")}
                      </button>
                    ) : null}
                    {!notification.read ? (
                      <button
                        className="secondary-button compact-button"
                        disabled={markingNotificationId === notification.id}
                        onClick={() => onMarkNotificationRead(notification.id)}
                        type="button"
                      >
                        {markingNotificationId === notification.id ? t("updating") : t("markAsRead")}
                      </button>
                    ) : (
                      <span className="read-state">{t("read")}</span>
                    )}
                  </div>
                </article>
              ))}
            </div>
          ) : null}
        </div>
      ) : null}
    </div>
  );
}

export default NotificationCenter;
