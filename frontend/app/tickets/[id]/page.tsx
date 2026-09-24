"use client";

import { FormEvent, useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import {
  addComment,
  ApiError,
  getTicket,
  transitionTicket,
  updateTicket,
} from "@/lib/api";
import {
  ErrorDetail,
  Priority,
  Ticket,
  TicketStatus,
} from "@/lib/types";

const NEXT_STATUSES: Record<TicketStatus, TicketStatus[]> = {
  OPEN: ["IN_PROGRESS", "CANCELLED"],
  IN_PROGRESS: ["RESOLVED", "CANCELLED"],
  RESOLVED: ["CLOSED"],
  CLOSED: [],
  CANCELLED: [],
};

const STATUS_LABELS: Record<TicketStatus, string> = {
  OPEN: "Open",
  IN_PROGRESS: "In progress",
  RESOLVED: "Resolved",
  CLOSED: "Closed",
  CANCELLED: "Cancelled",
};

const ACTION_LABELS: Partial<Record<TicketStatus, string>> = {
  IN_PROGRESS: "Start progress",
  RESOLVED: "Resolve ticket",
  CLOSED: "Close ticket",
  CANCELLED: "Cancel ticket",
};

export default function TicketDetailPage() {
  const { id } = useParams<{ id: string }>();
  const router = useRouter();
  const [ticket, setTicket] = useState<Ticket | null>(null);
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [priority, setPriority] = useState<Priority>("MEDIUM");
  const [assignee, setAssignee] = useState("");
  const [comment, setComment] = useState("");
  const [error, setError] = useState("");
  const [commentError, setCommentError] = useState("");
  const [details, setDetails] = useState<ErrorDetail[]>([]);
  const [notFound, setNotFound] = useState(false);
  const [saving, setSaving] = useState(false);
  const [commenting, setCommenting] = useState(false);
  const [transitioning, setTransitioning] = useState(false);

  useEffect(() => {
    getTicket(id)
      .then((loaded) => {
        applyTicket(loaded);
        setError("");
      })
      .catch((reason: unknown) => {
        if (reason instanceof ApiError) {
          setNotFound(reason.status === 404);
          setError(reason.body.message);
        } else {
          setError("Unable to load ticket");
        }
      });
  }, [id]);

  function applyTicket(updated: Ticket) {
    setTicket(updated);
    setTitle(updated.title);
    setDescription(updated.description);
    setPriority(updated.priority);
    setAssignee(updated.assignee ?? "");
  }

  async function save(event: FormEvent) {
    event.preventDefault();
    setSaving(true);
    try {
      const updated = await updateTicket(id, {
        title,
        description,
        priority,
        assignee: assignee.trim() || null,
      });
      applyTicket(updated);
      setError("");
      setDetails([]);
    } catch (reason) {
      if (reason instanceof ApiError) {
        setError(reason.body.message);
        setDetails(reason.body.details ?? []);
      } else {
        setError("Unable to update ticket");
      }
    } finally {
      setSaving(false);
    }
  }

  async function submitComment(event: FormEvent) {
    event.preventDefault();
    setCommenting(true);
    try {
      const added = await addComment(id, comment);
      setTicket((current) =>
        current ? { ...current, comments: [...current.comments, added] } : current,
      );
      setComment("");
      setCommentError("");
    } catch (reason) {
      setCommentError(
        reason instanceof ApiError ? reason.body.message : "Unable to add comment",
      );
    } finally {
      setCommenting(false);
    }
  }

  async function moveTo(target: TicketStatus) {
    setTransitioning(true);
    try {
      applyTicket(await transitionTicket(id, target));
      setError("");
    } catch (reason) {
      setError(
        reason instanceof ApiError ? reason.body.message : "Unable to change status",
      );
    } finally {
      setTransitioning(false);
    }
  }

  function fieldError(field: string) {
    return details.find((detail) => detail.field === field)?.message;
  }

  if (!ticket) {
    return (
      <section className="panel">
        <p>{error || "Loading ticket…"}</p>
        {notFound && (
          <button className="back-link" onClick={() => router.push(listHref())} type="button">
            ← Back to tickets
          </button>
        )}
      </section>
    );
  }

  return (
    <>
      <button className="back-link" onClick={() => router.push(listHref())} type="button">
        ← Back to tickets
      </button>
      <div className="page-heading compact">
        <div>
          <p className="eyebrow">Ticket detail</p>
          <h1>{ticket.title}</h1>
          <p className="muted">
            Updated {new Date(ticket.updatedAt).toLocaleString()} · {ticket.assignee ?? "Unassigned"}
          </p>
        </div>
        <span className={`status status-${ticket.status.toLowerCase()}`}>
          {STATUS_LABELS[ticket.status]}
        </span>
      </div>

      {error && <div className="error" role="alert">{error}</div>}

      <div className="detail-grid">
      <section className="panel detail-main">
        <div className="panel-heading">
          <div>
            <p className="eyebrow">Information</p>
            <h2>Ticket details</h2>
          </div>
        </div>
        <form onSubmit={save}>
          <label>
            <span>Title</span>
            <input onChange={(event) => setTitle(event.target.value)} value={title} />
            {fieldError("title") && (
              <small className="field-error">{fieldError("title")}</small>
            )}
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
          <div className="two-column">
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
            <span>Assignee</span>
            <input onChange={(event) => setAssignee(event.target.value)} value={assignee} />
            {fieldError("assignee") && (
              <small className="field-error">{fieldError("assignee")}</small>
            )}
          </label>
          </div>
          <button disabled={saving} type="submit">
            {saving ? "Saving…" : "Save changes"}
          </button>
        </form>
      </section>

      <aside>
      <section className="panel">
        <p className="eyebrow">Workflow</p>
        <h2>Status actions</h2>
        <div className="status-actions">
          {NEXT_STATUSES[ticket.status].map((target) => (
            <button
              className={target === "CANCELLED" ? "danger" : ""}
              disabled={transitioning}
              key={target}
              onClick={() => moveTo(target)}
              type="button"
            >
              {ACTION_LABELS[target]}
            </button>
          ))}
          {NEXT_STATUSES[ticket.status].length === 0 && (
            <p className="terminal-note">This ticket is in a terminal state.</p>
          )}
        </div>
      </section>
      </aside>
      </div>

      <section className="panel comments-panel">
        <div className="panel-heading">
          <div>
            <p className="eyebrow">Conversation</p>
            <h2>Comments</h2>
          </div>
          <span className="count-badge">{ticket.comments.length}</span>
        </div>
        {ticket.comments.length === 0 && <p className="muted">No comments yet.</p>}
        <div className="comment-list">
          {ticket.comments.map((item) => (
            <article className="comment" key={item.id}>
              <p>{item.body}</p>
              <small>{new Date(item.createdAt).toLocaleString()}</small>
            </article>
          ))}
        </div>
        <form className="comment-form" onSubmit={submitComment}>
          <label>
            <span>Add comment</span>
            <textarea
              onChange={(event) => setComment(event.target.value)}
              placeholder="Write an update…"
              rows={3}
              value={comment}
            />
          </label>
          {commentError && <div className="error" role="alert">{commentError}</div>}
          <button disabled={commenting} type="submit">
            {commenting ? "Adding…" : "Add comment"}
          </button>
        </form>
      </section>
    </>
  );
}

function listHref() {
  const query = sessionStorage.getItem("ticketListQuery");
  return query ? `/?${query}` : "/";
}
