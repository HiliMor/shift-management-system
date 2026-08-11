function CreateSchedulePanel({
  isCreatingSchedule,
  managedTeams,
  onCreateSchedule,
  onScheduleFormChange,
  scheduleForm,
}) {
  return (
    <section className="manager-panel">
      <h3>Create draft schedule</h3>
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
