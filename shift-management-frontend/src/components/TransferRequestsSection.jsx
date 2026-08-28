function isActiveRequest(request) {
  return request.status === "PENDING_EMPLOYEE" || request.status === "PENDING_MANAGER";
}

function isActingOnRequest(actingTransferRequest, requestId, actionName) {
  return actingTransferRequest?.id === requestId && actingTransferRequest.action === actionName;
}

function requestTypeLabel(type) {
  return type === "SWAP" ? "Swap" : "Transfer";
}

function requestStatusLabel(status) {
  return status
    .toLowerCase()
    .split("_")
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(" ");
}

function employeeOptionLabel(employee) {
  return `${employee.fullName || employee.username} (${employee.username})`;
}

function assignmentOptionLabel(option, formatDateTime, includeEmployee = false) {
  const shiftLabel = option.shift.description || `Shift #${option.shift.id}`;
  const employeeLabel = option.assignment.employeeFullName || option.assignment.employeeUsername;
  const owner = includeEmployee ? `${employeeLabel} - ` : "";

  return `#${option.assignment.id} - ${owner}${shiftLabel} (${formatDateTime(option.shift.startTime)})`;
}

function renderRequest(request, formatDateTime, actions = null) {
  return (
    <article className="request-row" key={request.id}>
      <div>
        <div className="request-title-row">
          <h3>
            {requestTypeLabel(request.type)} request #{request.id}
          </h3>
          <span className={`status-badge status-${request.status.toLowerCase().replaceAll("_", "-")}`}>
            {requestStatusLabel(request.status)}
          </span>
        </div>

        <div className="request-details">
          <div>
            <p className="eyebrow">From</p>
            <strong>{request.requesterFullName || request.requesterUsername}</strong>
            <span>{request.requesterUsername}</span>
          </div>
          <div>
            <p className="eyebrow">To</p>
            <strong>{request.targetEmployeeFullName || request.targetEmployeeUsername}</strong>
            <span>{request.targetEmployeeUsername}</span>
          </div>
          <div>
            <p className="eyebrow">Source assignment</p>
            <strong>#{request.sourceAssignmentId}</strong>
            <span>Shift #{request.sourceShiftId}</span>
          </div>
          {request.targetAssignmentId ? (
            <div>
              <p className="eyebrow">Target assignment</p>
              <strong>#{request.targetAssignmentId}</strong>
              <span>Shift #{request.targetShiftId}</span>
            </div>
          ) : null}
          <div>
            <p className="eyebrow">Created</p>
            <strong>{formatDateTime(request.createdAt)}</strong>
            <span>Updated {formatDateTime(request.updatedAt)}</span>
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
  const isCreatingSwap = transferRequestCreationForm.type === "SWAP";
  const hasSourceAssignments = sourceAssignmentOptions.length > 0;
  const hasTransferTargets = transferTargetEmployeeOptions.length > 0;
  const hasSwapTargets = swapTargetAssignmentOptions.length > 0;
  const canCreateRequest = hasSourceAssignments && (isCreatingSwap ? hasSwapTargets : hasTransferTargets);

  return (
    <section className="section-block" id="transfer-requests">
      <div className="section-heading">
        <h2>Transfer and swap requests</h2>
        <div className="section-actions">
          <span>{transferRequestCount}</span>
          <button
            className="secondary-button compact-button"
            disabled={isLoadingTransferRequests}
            onClick={onRefreshTransferRequests}
            type="button"
          >
            Refresh
          </button>
        </div>
      </div>

      {isLoadingTransferRequests ? <p className="muted">Loading requests...</p> : null}
      {transferRequestsError ? <p className="error-message">{transferRequestsError}</p> : null}
      {transferRequestActionError ? <p className="error-message">{transferRequestActionError}</p> : null}
      {transferRequestActionMessage ? <p className="success-message">{transferRequestActionMessage}</p> : null}

      {!isLoadingTransferRequests && !transferRequestsError && transferRequestCount === 0 ? (
        <p className="muted">No transfer or swap requests are available for this user.</p>
      ) : null}

      {isManager ? (
        <div className="request-section-stack">
          <section className="request-panel">
            <h3>Pending manager approval</h3>
            <div className="request-list">
              {pendingManagerTransferRequests.map((request) =>
                renderRequest(
                  request,
                  formatDateTime,
                  request.status === "PENDING_MANAGER" ? (
                    <button
                      className="compact-button"
                      disabled={actingTransferRequest !== null}
                      onClick={() => onApproveManagerTransferRequest(request.id)}
                      type="button"
                    >
                      {isActingOnRequest(actingTransferRequest, request.id, "manager-approve")
                        ? "Approving..."
                        : "Approve"}
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
            <h3>Create request</h3>

            {!selectedScheduleDetails ? (
              <p className="muted">Select a published schedule to create a request.</p>
            ) : null}
            {selectedScheduleDetails && !hasSourceAssignments ? (
              <p className="muted">The selected schedule does not include your assignments.</p>
            ) : null}

            {selectedScheduleDetails && hasSourceAssignments ? (
              <form className="transfer-request-form" onSubmit={onCreateTransferRequest}>
                <fieldset className="segmented-field">
                  <legend>Request type</legend>
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
                      <strong>Transfer</strong>
                      <span>Give away my assignment</span>
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
                      <strong>Swap</strong>
                      <span>Exchange two assignments</span>
                    </label>
                  </div>
                </fieldset>

                <label>
                  My assignment
                  <select
                    name="sourceAssignmentId"
                    onChange={onTransferRequestCreationFormChange}
                    required
                    value={transferRequestCreationForm.sourceAssignmentId}
                  >
                    {sourceAssignmentOptions.map((option) => (
                      <option key={option.assignment.id} value={option.assignment.id}>
                        {assignmentOptionLabel(option, formatDateTime)}
                      </option>
                    ))}
                  </select>
                </label>

                {isCreatingSwap ? (
                  <label>
                    Assignment to receive
                    <select
                      name="targetAssignmentId"
                      onChange={onTransferRequestCreationFormChange}
                      required
                      value={transferRequestCreationForm.targetAssignmentId}
                    >
                      {swapTargetAssignmentOptions.map((option) => (
                        <option key={option.assignment.id} value={option.assignment.id}>
                          {assignmentOptionLabel(option, formatDateTime, true)}
                        </option>
                      ))}
                    </select>
                  </label>
                ) : (
                  <label>
                    Employee receiving it
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
                  {isCreatingTransferRequest ? "Creating..." : "Create request"}
                </button>
              </form>
            ) : null}

            {selectedScheduleDetails && hasSourceAssignments && !hasTransferTargets && !isCreatingSwap ? (
              <p className="muted">No target employees are available in the selected schedule.</p>
            ) : null}
            {selectedScheduleDetails && hasSourceAssignments && !hasSwapTargets && isCreatingSwap ? (
              <p className="muted">No target assignments are available in the selected schedule.</p>
            ) : null}
            {transferRequestCreationError ? <p className="error-message">{transferRequestCreationError}</p> : null}
            {transferRequestCreationMessage ? <p className="success-message">{transferRequestCreationMessage}</p> : null}
          </section>

          <section className="request-panel">
            <h3>Incoming requests</h3>
            {incomingTransferRequests.length === 0 ? (
              <p className="muted">No incoming requests.</p>
            ) : null}
            <div className="request-list">
              {incomingTransferRequests.map((request) =>
                renderRequest(
                  request,
                  formatDateTime,
                  request.status === "PENDING_EMPLOYEE" ? (
                    <>
                      <button
                        className="compact-button"
                        disabled={actingTransferRequest !== null}
                        onClick={() => onApproveIncomingTransferRequest(request.id)}
                        type="button"
                      >
                        {isActingOnRequest(actingTransferRequest, request.id, "employee-approve")
                          ? "Approving..."
                          : "Approve"}
                      </button>
                      <button
                        className="secondary-button compact-button"
                        disabled={actingTransferRequest !== null}
                        onClick={() => onRejectIncomingTransferRequest(request.id)}
                        type="button"
                      >
                        {isActingOnRequest(actingTransferRequest, request.id, "employee-reject")
                          ? "Rejecting..."
                          : "Reject"}
                      </button>
                    </>
                  ) : null,
                ),
              )}
            </div>
          </section>

          <section className="request-panel">
            <h3>Outgoing requests</h3>
            {outgoingTransferRequests.length === 0 ? (
              <p className="muted">No outgoing requests.</p>
            ) : null}
            <div className="request-list">
              {outgoingTransferRequests.map((request) =>
                renderRequest(
                  request,
                  formatDateTime,
                  isActiveRequest(request) ? (
                    <button
                      className="secondary-button compact-button"
                      disabled={actingTransferRequest !== null}
                      onClick={() => onCancelOutgoingTransferRequest(request.id)}
                      type="button"
                    >
                      {isActingOnRequest(actingTransferRequest, request.id, "cancel")
                        ? "Cancelling..."
                        : "Cancel"}
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
