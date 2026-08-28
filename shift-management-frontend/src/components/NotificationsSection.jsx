import { useLanguage } from "../i18n/LanguageContext.jsx";

const notificationTypeTranslationKeys = {
  SCHEDULE_PUBLISHED: "schedulePublished",
};

function NotificationsSection({
  formatDateTime,
  isLoadingNotifications,
  markingNotificationId,
  notifications,
  notificationsError,
  onMarkNotificationRead,
  onRefreshNotifications,
  unreadNotificationCount,
}) {
  const { t } = useLanguage();

  function notificationTypeLabel(notification) {
    return t(notificationTypeTranslationKeys[notification.type] ?? notification.type);
  }

  function notificationTitle(notification) {
    return notification.type === "SCHEDULE_PUBLISHED"
      ? t("schedulePublished")
      : notification.title;
  }

  return (
    <section className="section-block" id="notifications">
      <div className="section-heading">
        <h2>{t("notifications")}</h2>
        <div className="section-actions">
          <span>
            {unreadNotificationCount} {t("unread")}
          </span>
          <button
            className="secondary-button compact-button"
            disabled={isLoadingNotifications}
            onClick={onRefreshNotifications}
            type="button"
          >
            {t("refresh")}
          </button>
        </div>
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
            </article>
          ))}
        </div>
      ) : null}
    </section>
  );
}

export default NotificationsSection;
