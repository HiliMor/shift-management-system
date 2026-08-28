function isActiveRequest(request) {
  return request.status === "PENDING_EMPLOYEE" || request.status === "PENDING_MANAGER";
}

function isActingOnRequest(actingTransferRequest, requestId, actionName) {
  return actingTransferRequest?.id === requestId && actingTransferRequest.action === actionName;
}

import { useLanguage } from "../i18n/LanguageContext.jsx";

const requestTypeTranslationKeys = {
  SWAP: "swap",
  TRANSFER: "transfer",
};

const requestStatusTranslationKeys = {
  PENDING_EMPLOYEE: "pendingEmployee",
  PENDING_MANAGER: "pendingManager",
  APPROVED: "approved",
  REJECTED: "rejected",
  CANCELLED: "cancelled",
  INVALIDATED: "invalidated",
};

function requestTypeLabel(type, t) {
  return t(requestTypeTranslationKeys[type] ?? type);
}

function requestStatusLabel(status, t) {
  return t(requestStatusTranslationKeys[status] ?? status);
}

function employeeOptionLabel(employee) {
  return `${employee.fullName || employee.username} (${employee.username})`;
}

function assignmentOptionLabel(option, formatDateTime, t, includeEmployee = false) {
  const shiftLabel = option.shift.description || `${t("shift")} #${option.shift.id}`;
  const employeeLabel = option.assignment.employeeFullName || option.assignment.employeeUsername;
  const owner = includeEmployee ? `${employeeLabel} - ` : "";

  return `#${option.assignment.id} - ${owner}${shiftLabel} (${formatDateTime(option.shift.startTime)})`;
}

function renderRequest(request, formatDateTime, t, actions = null) {
  return (
    <article className="request-row" key={request.id}>
      <div>
        <div className="request-title-row">
          <h3>
            {requestTypeLabel(request.type, t)} {t("request")} #{request.id}
          </h3>
          <span className={`status-badge status-${request.status.toLowerCase().replaceAll("_", "-")}`}>
            {requestStatusLabel(request.status, t)}
          </span>
        </div>

        <div className="request-details">
          <div>
            <p className="eyebrow">{t("from")}</p>
            <strong>{request.requesterFullName || request.requesterUsername}</strong>
            <span>{request.requesterUsername}</span>
          </div>
          <div>
            <p className="eyebrow">{t("to")}</p>
            <strong>{request.targetEmployeeFullName || request.targetEmployeeUsername}</strong>
            <span>{request.targetEmployeeUsername}</span>
          </div>
          <div>
            <p className="eyebrow">{t("sourceAssignment")}</p>
            <strong>#{request.sourceAssignmentId}</strong>
            <span>{t("shift")} #{request.sourceShiftId}</span>
          </div>
          {request.targetAssignmentId ? (
            <div>
              <p className="eyebrow">{t("targetAssignment")}</p>
              <strong>#{request.targetAssignmentId}</strong>
              <span>{t("shift")} #{request.targetShiftId}</span>
            </div>
          ) : null}
          <div>
            <p className="eyebrow">{t("created")}</p>
            <strong>{formatDateTime(request.createdAt)}</strong>
            <span>{t("updated")} {formatDateTime(request.updatedAt)}</span>
          </div>
        </div>
      </div>

      {actions ? <div className="request-actions">{actions}</div> : null}
    </article>
  );
}

function TransferRequestsSection({
  actingTransferRequest,
  formatDateTime,
  incomingTransferRequests,
  isCreatingTransferRequest,
  isLoadingTransferRequests,
  isManager,
  onApproveIncomingTransferRequest,
  onApproveManagerTransferRequest,
  onCancelOutgoingTransferRequest,
  onCreateTransferRequest,
  onTransferRequestCreationFormChange,
  onRefreshTransferRequests,
  onRejectIncomingTransferRequest,
  outgoingTransferRequests,
  pendingManagerTransferRequests,
  selectedScheduleDetails,
  sourceAssignmentOptions,
  swapTargetAssignmentOptions,
  transferRequestCreationError,
  transferRequestCreationForm,
  transferRequestCreationMessage,
  transferRequestActionError,
  transferRequestActionMessage,
  transferRequestCount,
  transferRequestsError,
  transferTargetEmployeeOptions,
}) {
  const { t } = useLanguage();
  const isCreatingSwap = transferRequestCreationForm.type === "SWAP";
  const hasSourceAssignments = sourceAssignmentOptions.length > 0;
  const hasTransferTargets = transferTargetEmployeeOptions.length > 0;
  const hasSwapTargets = swapTargetAssignmentOptions.length > 0;
  const canCreateRequest = hasSourceAssignments && (isCreatingSwap ? hasSwapTargets : hasTransferTargets);

  return (
    <section className="section-block" id="transfer-requests">
      <div className="section-heading">
        <h2>{t("transferAndSwapRequests")}</h2>
        <div className="section-actions">
          <span>{transferRequestCount}</span>
          <button
            className="secondary-button compact-button"
            disabled={isLoadingTransferRequests}
            onClick={onRefreshTransferRequests}
            type="button"
          >
            {t("refresh")}
          </button>
        </div>
      </div>

      {isLoadingTransferRequests ? <p className="muted">{t("loadingRequests")}</p> : null}
      {transferRequestsError ? <p className="error-message">{transferRequestsError}</p> : null}
      {transferRequestActionError ? <p className="error-message">{transferRequestActionError}</p> : null}
      {transferRequestActionMessage ? <p className="success-message">{t(transferRequestActionMessage)}</p> : null}

      {!isLoadingTransferRequests && !transferRequestsError && transferRequestCount === 0 ? (
        <p className="muted">{t("noTransferSwapRequests")}</p>
      ) : null}

      {isManager ? (
        <div className="request-section-stack">
          <section className="request-panel">
            <h3>{t("pendingManagerApproval")}</h3>
            <div className="request-list">
              {pendingManagerTransferRequests.map((request) =>
                renderRequest(
                  request,
                  formatDateTime,
                  t,
                  request.status === "PENDING_MANAGER" ? (
                    <button
                      className="compact-button"
                      disabled={actingTransferRequest !== null}
                      onClick={() => onApproveManagerTransferRequest(request.id)}
                      type="button"
                    >
                      {isActingOnRequest(actingTransferRequest, request.id, "manager-approve")
                        ? t("approving")
                        : t("approve")}
                    </button>
                  ) : null,
                ),
              )}
            </div>
          </section>
        </div>
      ) : (
        <div className="request-section-stack">
          <section className="request-panel">
            <h3>{t("createRequest")}</h3>

            {!selectedScheduleDetails ? (
              <p className="muted">{t("selectPublishedScheduleForRequest")}</p>
            ) : null}
            {selectedScheduleDetails && !hasSourceAssignments ? (
              <p className="muted">{t("noOwnAssignments")}</p>
            ) : null}

            {selectedScheduleDetails && hasSourceAssignments ? (
              <form className="transfer-request-form" onSubmit={onCreateTransferRequest}>
                <fieldset className="segmented-field">
                  <legend>{t("requestType")}</legend>
                  <div className="segmented-control">
                    <label
                      className={
                        transferRequestCreationForm.type === "TRANSFER"
                          ? "segmented-option selected-segment"
                          : "segmented-option"
                      }
                    >
                      <input
                        checked={transferRequestCreationForm.type === "TRANSFER"}
                        name="type"
                        onChange={onTransferRequestCreationFormChange}
                        type="radio"
                        value="TRANSFER"
                      />
                      <strong>{t("transfer")}</strong>
                      <span>{t("giveAwayAssignment")}</span>
                    </label>
                    <label
                      className={
                        transferRequestCreationForm.type === "SWAP"
                          ? "segmented-option selected-segment"
                          : "segmented-option"
                      }
                    >
                      <input
                        checked={transferRequestCreationForm.type === "SWAP"}
                        name="type"
                        onChange={onTransferRequestCreationFormChange}
                        type="radio"
                        value="SWAP"
                      />
                      <strong>{t("swap")}</strong>
                      <span>{t("exchangeAssignments")}</span>
                    </label>
                  </div>
                </fieldset>

                <label>
                  {t("myAssignment")}
                  <select
                    name="sourceAssignmentId"
                    onChange={onTransferRequestCreationFormChange}
                    required
                    value={transferRequestCreationForm.sourceAssignmentId}
                  >
                    {sourceAssignmentOptions.map((option) => (
                      <option key={option.assignment.id} value={option.assignment.id}>
                        {assignmentOptionLabel(option, formatDateTime, t)}
                      </option>
                    ))}
                  </select>
                </label>

                {isCreatingSwap ? (
                  <label>
                    {t("assignmentToReceive")}
                    <select
                      name="targetAssignmentId"
                      onChange={onTransferRequestCreationFormChange}
                      required
                      value={transferRequestCreationForm.targetAssignmentId}
                    >
                      {swapTargetAssignmentOptions.map((option) => (
                        <option key={option.assignment.id} value={option.assignment.id}>
                          {assignmentOptionLabel(option, formatDateTime, t, true)}
                        </option>
                      ))}
                    </select>
                  </label>
                ) : (
                  <label>
                    {t("employeeReceiving")}
                    <select
                      name="targetEmployeeId"
                      onChange={onTransferRequestCreationFormChange}
                      required
                      value={transferRequestCreationForm.targetEmployeeId}
                    >
                      {transferTargetEmployeeOptions.map((employee) => (
                        <option key={employee.id} value={employee.id}>
                          {employeeOptionLabel(employee)}
                        </option>
                      ))}
                    </select>
                  </label>
                )}

                <button disabled={!canCreateRequest || isCreatingTransferRequest} type="submit">
                  {isCreatingTransferRequest ? t("creating") : t("createRequest")}
                </button>
              </form>
            ) : null}

            {selectedScheduleDetails && hasSourceAssignments && !hasTransferTargets && !isCreatingSwap ? (
              <p className="muted">{t("noTargetEmployees")}</p>
            ) : null}
            {selectedScheduleDetails && hasSourceAssignments && !hasSwapTargets && isCreatingSwap ? (
              <p className="muted">{t("noTargetAssignments")}</p>
            ) : null}
            {transferRequestCreationError ? <p className="error-message">{transferRequestCreationError}</p> : null}
            {transferRequestCreationMessage ? <p className="success-message">{t(transferRequestCreationMessage)}</p> : null}
          </section>

          <section className="request-panel">
            <h3>{t("incomingRequests")}</h3>
            {incomingTransferRequests.length === 0 ? (
              <p className="muted">{t("noIncomingRequests")}</p>
            ) : null}
            <div className="request-list">
              {incomingTransferRequests.map((request) =>
                renderRequest(
                  request,
                  formatDateTime,
                  t,
                  request.status === "PENDING_EMPLOYEE" ? (
                    <>
                      <button
                        className="compact-button"
                        disabled={actingTransferRequest !== null}
                        onClick={() => onApproveIncomingTransferRequest(request.id)}
                        type="button"
                      >
                        {isActingOnRequest(actingTransferRequest, request.id, "employee-approve")
                          ? t("approving")
                          : t("approve")}
                      </button>
                      <button
                        className="secondary-button compact-button"
                        disabled={actingTransferRequest !== null}
                        onClick={() => onRejectIncomingTransferRequest(request.id)}
                        type="button"
                      >
                        {isActingOnRequest(actingTransferRequest, request.id, "employee-reject")
                          ? t("rejecting")
                          : t("reject")}
                      </button>
                    </>
                  ) : null,
                ),
              )}
            </div>
          </section>

          <section className="request-panel">
            <h3>{t("outgoingRequests")}</h3>
            {outgoingTransferRequests.length === 0 ? (
              <p className="muted">{t("noOutgoingRequests")}</p>
            ) : null}
            <div className="request-list">
              {outgoingTransferRequests.map((request) =>
                renderRequest(
                  request,
                  formatDateTime,
                  t,
                  isActiveRequest(request) ? (
                    <button
                      className="secondary-button compact-button"
                      disabled={actingTransferRequest !== null}
                      onClick={() => onCancelOutgoingTransferRequest(request.id)}
                      type="button"
                    >
                      {isActingOnRequest(actingTransferRequest, request.id, "cancel")
                        ? t("cancelling")
                        : t("cancel")}
                    </button>
                  ) : null,
                ),
              )}
            </div>
          </section>
        </div>
      )}
    </section>
  );
}

export default TransferRequestsSection;
