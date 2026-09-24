"use client";

import { FormEvent, useState } from "react";
import { useRouter } from "next/navigation";
import { ApiError, createTicket } from "@/lib/api";
import { ErrorDetail, Priority } from "@/lib/types";

export default function NewTicketPage() {
  const router = useRouter();
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [priority, setPriority] = useState<Priority>("MEDIUM");
  const [assignee, setAssignee] = useState("");
  const [error, setError] = useState("");
  const [details, setDetails] = useState<ErrorDetail[]>([]);
  const [submitting, setSubmitting] = useState(false);

  async function submit(event: FormEvent) {
    event.preventDefault();
    setSubmitting(true);
    try {
      const ticket = await createTicket({
        title,
        description,
        priority,
        assignee: assignee.trim() || null,
      });
      router.push(`/tickets/${ticket.id}`);
    } catch (reason) {
      if (reason instanceof ApiError) {
        setError(reason.body.message);
        setDetails(reason.body.details ?? []);
      } else {
        setError("Unable to create ticket");
      }
    } finally {
      setSubmitting(false);
    }
  }

  function fieldError(field: string) {
    return details.find((detail) => detail.field === field)?.message;
  }

  return (
    <section className="form-page">
      <button className="back-link" onClick={() => router.push(listHref())} type="button">
        ← Back to tickets
      </button>
      <div className="page-heading compact">
        <div>
          <p className="eyebrow">Create request</p>
          <h1>New ticket</h1>
          <p className="muted">Capture the issue and set its initial priority.</p>
        </div>
      </div>
      {error && <div className="error" role="alert">{error}</div>}
      <form className="panel form-card" onSubmit={submit}>
        <h2>Ticket details</h2>
        <label>
          <span>Title</span>
          <input onChange={(event) => setTitle(event.target.value)} value={title} />
          {fieldError("title") && <small className="field-error">{fieldError("title")}</small>}
        </label>
        <label>
          <span>Description</span>
          <textarea
            onChange={(event) => setDescription(event.target.value)}
            rows={6}
            value={description}
          />
          {fieldError("description") && (
            <small className="field-error">{fieldError("description")}</small>
          )}
        </label>
        <label>
          <span>Priority</span>
          <select
            onChange={(event) => setPriority(event.target.value as Priority)}
            value={priority}
          >
            <option value="LOW">LOW</option>
            <option value="MEDIUM">MEDIUM</option>
            <option value="HIGH">HIGH</option>
          </select>
          {fieldError("priority") && (
            <small className="field-error">{fieldError("priority")}</small>
          )}
        </label>
        <label>
          <span>Assignee (optional)</span>
          <input onChange={(event) => setAssignee(event.target.value)} value={assignee} />
          {fieldError("assignee") && (
            <small className="field-error">{fieldError("assignee")}</small>
          )}
        </label>
        <div className="actions">
          <button disabled={submitting} type="submit">
            {submitting ? "Creating…" : "Create ticket"}
          </button>
          <button className="secondary" onClick={() => router.push(listHref())} type="button">
            Cancel
          </button>
        </div>
      </form>
    </section>
  );
}

function listHref() {
  const query = sessionStorage.getItem("ticketListQuery");
  return query ? `/?${query}` : "/";
}
