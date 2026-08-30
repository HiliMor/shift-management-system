import { useLanguage } from "../i18n/LanguageContext.jsx";

function AvailabilityConstraintsSection({
  availabilityActionError,
  availabilityActionMessage,
  availabilityConstraints,
  availabilityError,
  availabilityForm,
  deletingAvailabilityId,
  formatDateTime,
  isCreatingAvailability,
  isLoadingAvailability,
  onAvailabilityFormChange,
  onCreateAvailabilityConstraint,
  onDeleteAvailabilityConstraint,
  onRefreshAvailabilityConstraints,
}) {
  const { t } = useLanguage();

  return (
    <section className="section-block" id="availability">
      <div className="section-heading">
        <div>
          <h2>{t("myAvailability")}</h2>
          <p className="section-description">{t("availabilityDescription")}</p>
        </div>
        <div className="section-actions">
          <span>{availabilityConstraints.length}</span>
          <button
            className="secondary-button compact-button"
            disabled={isLoadingAvailability}
            onClick={onRefreshAvailabilityConstraints}
            type="button"
          >
            {t("refresh")}
          </button>
        </div>
      </div>

      <form className="availability-form" onSubmit={onCreateAvailabilityConstraint}>
        <label className="checkbox-field">
          <input
            checked={availabilityForm.allDay}
            name="allDay"
            onChange={onAvailabilityFormChange}
            type="checkbox"
          />
          {t("fullDay")}
        </label>

        {availabilityForm.allDay ? (
          <label>
            {t("date")}
            <input
              name="date"
              onChange={onAvailabilityFormChange}
              required
              type="date"
              value={availabilityForm.date}
            />
          </label>
        ) : (
          <>
            <label>
              {t("startTime")}
              <input
                name="startTime"
                onChange={onAvailabilityFormChange}
                required
                type="datetime-local"
                value={availabilityForm.startTime}
              />
            </label>

            <label>
              {t("endTime")}
              <input
                name="endTime"
                onChange={onAvailabilityFormChange}
                required
                type="datetime-local"
                value={availabilityForm.endTime}
              />
            </label>
          </>
        )}

        <label>
          {t("reason")}
          <input
            maxLength="500"
            name="reason"
            onChange={onAvailabilityFormChange}
            placeholder={t("optional")}
            type="text"
            value={availabilityForm.reason}
          />
        </label>

        <button disabled={isCreatingAvailability} type="submit">
          {isCreatingAvailability ? t("saving") : t("addConstraint")}
        </button>
      </form>

      {availabilityActionError ? <p className="error-message">{availabilityActionError}</p> : null}
      {availabilityActionMessage ? <p className="success-message">{t(availabilityActionMessage)}</p> : null}
      {isLoadingAvailability ? <p className="muted">{t("loadingAvailability")}</p> : null}
      {availabilityError ? <p className="error-message">{availabilityError}</p> : null}

      {!isLoadingAvailability && !availabilityError && availabilityConstraints.length === 0 ? (
        <p className="muted">{t("noAvailabilityConstraints")}</p>
      ) : null}

      {availabilityConstraints.length > 0 ? (
        <div className="availability-list">
          {availabilityConstraints.map((constraint) => (
            <article className="availability-row" key={constraint.id}>
              <div>
                <h3>
                  {formatDateTime(constraint.startTime)} {t("dateRangeSeparator")} {formatDateTime(constraint.endTime)}
                </h3>
                <p>{constraint.reason || t("noReasonProvided")}</p>
                <p className="notification-meta">
                  {t("created")} {formatDateTime(constraint.createdAt)}
                </p>
              </div>

              <button
                className="secondary-button compact-button"
                disabled={deletingAvailabilityId !== null}
                onClick={() => onDeleteAvailabilityConstraint(constraint.id)}
                type="button"
              >
                {deletingAvailabilityId === constraint.id ? t("deleting") : t("delete")}
              </button>
            </article>
          ))}
        </div>
      ) : null}
    </section>
  );
}

export default AvailabilityConstraintsSection;
