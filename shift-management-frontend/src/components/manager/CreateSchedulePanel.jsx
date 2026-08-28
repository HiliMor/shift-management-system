function CreateSchedulePanel({
  isCreatingSchedule,
  managedTeams,
  onCreateSchedule,
  onScheduleFormChange,
  scheduleForm,
}) {
  return (
    <section className="manager-panel" id="manager-drafts">
      <div className="manager-panel-heading">
        <span>1</span>
        <h3>Draft schedule</h3>
      </div>
      <form className="manager-form" onSubmit={onCreateSchedule}>
        <label>
          Team
          <select name="teamId" onChange={onScheduleFormChange} required value={scheduleForm.teamId}>
            {managedTeams.map((team) => (
              <option key={team.id} value={team.id}>
                {team.name}
              </option>
            ))}
          </select>
        </label>

        <label>
          Start date
          <input
            name="startDate"
            onChange={onScheduleFormChange}
            required
            type="date"
            value={scheduleForm.startDate}
          />
        </label>

        <label>
          End date
          <input
            name="endDate"
            onChange={onScheduleFormChange}
            required
            type="date"
            value={scheduleForm.endDate}
          />
        </label>

        <button disabled={isCreatingSchedule} type="submit">
          {isCreatingSchedule ? "Creating..." : "Create draft schedule"}
        </button>
      </form>
    </section>
  );
}

export default CreateSchedulePanel;
