import {
  ApiErrorBody,
  Comment,
  PageResponse,
  Priority,
  Ticket,
  TicketStatus,
  TicketSummary,
} from "@/lib/types";

const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080/api/v1";

export class ApiError extends Error {
  constructor(
    public readonly status: number,
    public readonly body: ApiErrorBody,
  ) {
    super(body.message);
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers: {
      "Content-Type": "application/json",
      ...init?.headers,
    },
  });

  if (!response.ok) {
    let body: ApiErrorBody;
    try {
      body = (await response.json()) as ApiErrorBody;
    } catch {
      body = {
        code: "REQUEST_FAILED",
        message: `Request failed with status ${response.status}`,
        details: [],
      };
    }
    throw new ApiError(response.status, body);
  }

  return (await response.json()) as T;
}

export function listTickets(query: URLSearchParams) {
  return request<PageResponse<TicketSummary>>(`/tickets?${query.toString()}`);
}

export function getTicket(id: string) {
  return request<Ticket>(`/tickets/${id}`);
}

export function createTicket(input: {
  title: string;
  description: string;
  priority: Priority;
  assignee: string | null;
}) {
  return request<Ticket>("/tickets", {
    method: "POST",
    body: JSON.stringify(input),
  });
}

export function updateTicket(
  id: string,
  input: {
    title: string;
    description: string;
    priority: Priority;
    assignee: string | null;
  },
) {
  return request<Ticket>(`/tickets/${id}`, {
    method: "PATCH",
    body: JSON.stringify(input),
  });
}

export function addComment(id: string, body: string) {
  return request<Comment>(`/tickets/${id}/comments`, {
    method: "POST",
    body: JSON.stringify({ body }),
  });
}

export function transitionTicket(id: string, status: TicketStatus) {
  return request<Ticket>(`/tickets/${id}/status`, {
    method: "POST",
    body: JSON.stringify({ status }),
  });
}
