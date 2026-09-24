"use client";

import Link from "next/link";
import { FormEvent, useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { ApiError, listTickets } from "@/lib/api";
import { PageResponse, TicketStatus, TicketSummary } from "@/lib/types";

const STATUSES: TicketStatus[] = [
  "OPEN",
  "IN_PROGRESS",
  "RESOLVED",
  "CLOSED",
  "CANCELLED",
];

const STATUS_LABELS: Record<TicketStatus, string> = {
  OPEN: "Open",
  IN_PROGRESS: "In progress",
  RESOLVED: "Resolved",
  CLOSED: "Closed",
  CANCELLED: "Cancelled",
};

export default function TicketList() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [keyword, setKeyword] = useState(searchParams.get("q") ?? "");
  const [status, setStatus] = useState(searchParams.get("status") ?? "");
  const [page, setPage] = useState<PageResponse<TicketSummary> | null>(null);
  const [error, setError] = useState("");

  useEffect(() => {
    setKeyword(searchParams.get("q") ?? "");
    setStatus(searchParams.get("status") ?? "");
    sessionStorage.setItem("ticketListQuery", searchParams.toString());
    setPage(null);
    setError("");

    const query = new URLSearchParams(searchParams.toString());
    if (!query.has("size")) query.set("size", "20");
    listTickets(query)
      .then((response) => {
        setPage(response);
        setError("");
      })
      .catch((reason: unknown) => {
        setError(reason instanceof ApiError ? reason.body.message : "Unable to load tickets");
      });
  }, [searchParams]);

  function submitFilters(event: FormEvent) {
    event.preventDefault();
    const query = new URLSearchParams();
    if (keyword.trim()) query.set("q", keyword.trim());
    if (status) query.set("status", status);
    router.push(`/?${query.toString()}`);
  }

  function changePage(nextPage: number) {
    const query = new URLSearchParams(searchParams.toString());
    query.set("page", String(nextPage));
    router.push(`/?${query.toString()}`);
  }

  return (
    <>
      <div className="page-heading">
        <div>
          <p className="eyebrow">Support workspace</p>
          <h1>Tickets</h1>
          <p className="muted">Find, triage, and resolve customer requests.</p>
        </div>
        <Link className="button" href="/tickets/new">
          + New ticket
        </Link>
      </div>

      <form className="filter-bar" onSubmit={submitFilters}>
        <label className="filter-field">
          <span>Search</span>
          <input
            onChange={(event) => setKeyword(event.target.value)}
            placeholder="Title or description"
            value={keyword}
          />
        </label>
        <label className="filter-field filter-status">
          <span>Status</span>
          <select
            onChange={(event) => setStatus(event.target.value)}
            value={status}
          >
            <option value="">All statuses</option>
            {STATUSES.map((value) => (
              <option key={value} value={value}>
                {STATUS_LABELS[value]}
              </option>
            ))}
          </select>
        </label>
        <button type="submit">Apply</button>
      </form>

      {error && <div className="error" role="alert">{error}</div>}
      {!error && !page && <div className="empty-state">Loading tickets…</div>}
      {!error && page?.content.length === 0 && (
        <div className="empty-state">
          <strong>No tickets</strong>
          <span>Try changing your search or create a new ticket.</span>
        </div>
      )}
      {!error && page && page.content.length > 0 && (
        <>
          <div className="table-card">
            <table>
              <thead>
                <tr>
                  <th>Ticket</th>
                  <th>Status</th>
                  <th>Priority</th>
                  <th>Assignee</th>
                  <th>Updated</th>
                </tr>
              </thead>
              <tbody>
                {page.content.map((ticket) => (
                  <tr
                    className="ticket-row"
                    key={ticket.id}
                    onClick={() => router.push(`/tickets/${ticket.id}`)}
                    onKeyDown={(event) => {
                      if (event.key === "Enter" || event.key === " ") {
                        router.push(`/tickets/${ticket.id}`);
                      }
                    }}
                    role="link"
                    tabIndex={0}
                  >
                    <td>
                      <strong>{ticket.title}</strong>
                      <small>{ticket.description}</small>
                    </td>
                    <td>
                      <span className={`status status-${ticket.status.toLowerCase()}`}>
                        {STATUS_LABELS[ticket.status]}
                      </span>
                    </td>
                    <td>
                      <span className={`priority priority-${ticket.priority.toLowerCase()}`}>
                        {ticket.priority}
                      </span>
                    </td>
                    <td>{ticket.assignee ?? "Unassigned"}</td>
                    <td>{new Date(ticket.updatedAt).toLocaleString()}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="pagination">
            <button
              className="secondary"
              disabled={page.page === 0}
              onClick={() => changePage(page.page - 1)}
              type="button"
            >
              Previous
            </button>
            <span>
              Page {page.page + 1} of {Math.max(page.totalPages, 1)}
            </span>
            <button
              className="secondary"
              disabled={page.page + 1 >= page.totalPages}
              onClick={() => changePage(page.page + 1)}
              type="button"
            >
              Next
            </button>
          </div>
        </>
      )}
    </>
  );
}
