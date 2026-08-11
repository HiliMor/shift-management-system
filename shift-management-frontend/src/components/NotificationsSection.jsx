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
  return (
    <section className="section-block" id="notifications">
      <div className="section-heading">
        <h2>Notifications</h2>
        <div className="section-actions">
          <span>{unreadNotificationCount} unread</span>
          <button
            className="secondary-button compact-button"
            disabled={isLoadingNotifications}
            onClick={onRefreshNotifications}
            type="button"
          >
            Refresh
          </button>
        </div>
      </div>

      {isLoadingNotifications ? <p className="muted">Loading notifications...</p> : null}
      {notificationsError ? <p className="error-message">{notificationsError}</p> : null}

      {!isLoadingNotifications && !notificationsError && notifications.length === 0 ? (
        <p className="muted">No notifications are available for this user.</p>
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
                  <h3>{notification.title}</h3>
                  {!notification.read ? <span>Unread</span> : null}
                </div>
                <p>{notification.message}</p>
                <p className="notification-meta">
                  {notification.type} - {formatDateTime(notification.createdAt)}
                </p>
              </div>

              {!notification.read ? (
                <button
                  className="secondary-button compact-button"
                  disabled={markingNotificationId === notification.id}
                  onClick={() => onMarkNotificationRead(notification.id)}
                  type="button"
                >
                  {markingNotificationId === notification.id ? "Updating..." : "Mark as read"}
                </button>
              ) : (
                <span className="read-state">Read</span>
              )}
            </article>
          ))}
        </div>
      ) : null}
    </section>
  );
}

export default NotificationsSection;
