function isActiveTransferRequest(request) {
  return request.status === "PENDING_EMPLOYEE" || request.status === "PENDING_MANAGER";
}

function isActingOnTransferRequest(actingTransferRequest, requestId, actionName) {
  return actingTransferRequest?.id === requestId && actingTransferRequest.action === actionName;
}

function renderTransferRequest(request, formatDateTime, actions = null) {
  return (
    <article className="request-row" key={request.id}>
      <div>
        <div className="request-title-row">
          <h3>Transfer request #{request.id}</h3>
          <span className={`status-badge status-${request.status.toLowerCase().replaceAll("_", "-")}`}>
            {request.status}
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
            <p className="eyebrow">Assignment</p>
            <strong>#{request.sourceAssignmentId}</strong>
            <span>Shift #{request.sourceShiftId}</span>
          </div>
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
  isLoadingTransferRequests,
  isManager,
  onApproveIncomingTransferRequest,
  onApproveManagerTransferRequest,
  onCancelOutgoingTransferRequest,
  onRefreshTransferRequests,
  onRejectIncomingTransferRequest,
  outgoingTransferRequests,
  pendingManagerTransferRequests,
  transferRequestActionError,
  transferRequestActionMessage,
  transferRequestCount,
  transferRequestsError,
}) {
  return (
    <section className="section-block" id="transfer-requests">
      <div className="section-heading">
        <h2>Transfer requests</h2>
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

      {isLoadingTransferRequests ? <p className="muted">Loading transfer requests...</p> : null}
      {transferRequestsError ? <p className="error-message">{transferRequestsError}</p> : null}
      {transferRequestActionError ? <p className="error-message">{transferRequestActionError}</p> : null}
      {transferRequestActionMessage ? <p className="success-message">{transferRequestActionMessage}</p> : null}

      {!isLoadingTransferRequests && !transferRequestsError && transferRequestCount === 0 ? (
        <p className="muted">No transfer requests are available for this user.</p>
      ) : null}

      {isManager ? (
        <div className="request-section-stack">
          <section className="request-panel">
            <h3>Pending manager approval</h3>
            <div className="request-list">
              {pendingManagerTransferRequests.map((request) =>
                renderTransferRequest(
                  request,
                  formatDateTime,
                  request.status === "PENDING_MANAGER" ? (
                    <button
                      className="compact-button"
                      disabled={actingTransferRequest !== null}
                      onClick={() => onApproveManagerTransferRequest(request.id)}
                      type="button"
                    >
                      {isActingOnTransferRequest(actingTransferRequest, request.id, "manager-approve")
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
            <h3>Incoming requests</h3>
            {incomingTransferRequests.length === 0 ? (
              <p className="muted">No incoming transfer requests.</p>
            ) : null}
            <div className="request-list">
              {incomingTransferRequests.map((request) =>
                renderTransferRequest(
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
                        {isActingOnTransferRequest(actingTransferRequest, request.id, "employee-approve")
                          ? "Approving..."
                          : "Approve"}
                      </button>
                      <button
                        className="secondary-button compact-button"
                        disabled={actingTransferRequest !== null}
                        onClick={() => onRejectIncomingTransferRequest(request.id)}
                        type="button"
                      >
                        {isActingOnTransferRequest(actingTransferRequest, request.id, "employee-reject")
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
              <p className="muted">No outgoing transfer requests.</p>
            ) : null}
            <div className="request-list">
              {outgoingTransferRequests.map((request) =>
                renderTransferRequest(
                  request,
                  formatDateTime,
                  isActiveTransferRequest(request) ? (
                    <button
                      className="secondary-button compact-button"
                      disabled={actingTransferRequest !== null}
                      onClick={() => onCancelOutgoingTransferRequest(request.id)}
                      type="button"
                    >
                      {isActingOnTransferRequest(actingTransferRequest, request.id, "cancel")
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
