CREATE UNIQUE INDEX uk_shifts_generated_template_slot_start
    ON shifts(schedule_id, template_slot_id, start_time)
    WHERE template_slot_id IS NOT NULL;
