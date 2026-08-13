import { useEffect, useState } from "react";

import {
  countMyUnreadNotifications,
  listMyNotifications,
  markNotificationRead,
} from "../api.js";

function useNotifications(session, onApiError) {
  const [notifications, setNotifications] = useState([]);
  const [unreadNotificationCount, setUnreadNotificationCount] = useState(0);
  const [notificationsError, setNotificationsError] = useState("");
  const [isLoadingNotifications, setIsLoadingNotifications] = useState(false);
  const [markingNotificationId, setMarkingNotificationId] = useState(null);
  const [notificationRefreshKey, setNotificationRefreshKey] = useState(0);

  useEffect(() => {
    if (!session?.accessToken) {
      setNotifications([]);
      setUnreadNotificationCount(0);
      setNotificationsError("");
      return;
    }

    setIsLoadingNotifications(true);
    setNotificationsError("");

    Promise.all([
      listMyNotifications(session.accessToken),
      countMyUnreadNotifications(session.accessToken),
    ])
      .then(([notificationList, unreadCount]) => {
        setNotifications(notificationList);
        setUnreadNotificationCount(unreadCount.unreadCount);
      })
      .catch((error) => onApiError(error, setNotificationsError))
      .finally(() => setIsLoadingNotifications(false));
  }, [notificationRefreshKey, session]);

  async function markNotificationAsRead(notificationId) {
    setMarkingNotificationId(notificationId);
    setNotificationsError("");

    try {
      const updatedNotification = await markNotificationRead(session.accessToken, notificationId);
      const wasUnread = notifications.some(
        (notification) => notification.id === updatedNotification.id && !notification.read,
      );

      setNotifications((current) =>
        current.map((notification) =>
          notification.id === updatedNotification.id ? updatedNotification : notification,
        ),
      );
      if (wasUnread) {
        setUnreadNotificationCount((current) => Math.max(0, current - 1));
      }
    } catch (error) {
      onApiError(error, setNotificationsError);
    } finally {
      setMarkingNotificationId(null);
    }
  }

  function refreshNotifications() {
    setNotificationRefreshKey((current) => current + 1);
  }

  function resetNotifications() {
    setNotifications([]);
    setUnreadNotificationCount(0);
    setNotificationsError("");
    setIsLoadingNotifications(false);
    setMarkingNotificationId(null);
    setNotificationRefreshKey(0);
  }

  return {
    isLoadingNotifications,
    markingNotificationId,
    markNotificationAsRead,
    notifications,
    notificationsError,
    refreshNotifications,
    resetNotifications,
    unreadNotificationCount,
  };
}

export default useNotifications;
