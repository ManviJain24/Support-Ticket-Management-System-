package com.ticketing.dto.request;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.ticketing.domain.Priority;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UpdateTicketRequest {

    @Pattern(regexp = ".*\\S.*", message = "must not be blank")
    @Size(max = 200)
    private String title;

    @Pattern(regexp = "(?s).*\\S.*", message = "must not be blank")
    private String description;

    private Priority priority;

    @Size(max = 200)
    private String assignee;

    private boolean titlePresent;
    private boolean descriptionPresent;
    private boolean priorityPresent;
    private boolean assigneePresent;

    public String getTitle() {
        return title;
    }

    @JsonSetter
    public void setTitle(String title) {
        this.title = title;
        this.titlePresent = true;
    }

    public String getDescription() {
        return description;
    }

    @JsonSetter
    public void setDescription(String description) {
        this.description = description;
        this.descriptionPresent = true;
    }

    public Priority getPriority() {
        return priority;
    }

    @JsonSetter
    public void setPriority(Priority priority) {
        this.priority = priority;
        this.priorityPresent = true;
    }

    public String getAssignee() {
        return assignee;
    }

    @JsonSetter
    public void setAssignee(String assignee) {
        this.assignee = assignee;
        this.assigneePresent = true;
    }

    public boolean isTitlePresent() {
        return titlePresent;
    }

    public boolean isDescriptionPresent() {
        return descriptionPresent;
    }

    public boolean isPriorityPresent() {
        return priorityPresent;
    }

    public boolean isAssigneePresent() {
        return assigneePresent;
    }
}
