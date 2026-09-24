export type TicketStatus =
  | "OPEN"
  | "IN_PROGRESS"
  | "RESOLVED"
  | "CLOSED"
  | "CANCELLED";

export type Priority = "LOW" | "MEDIUM" | "HIGH";

export interface Comment {
  id: string;
  body: string;
  createdAt: string;
}

export interface TicketSummary {
  id: string;
  title: string;
  description: string;
  status: TicketStatus;
  priority: Priority;
  assignee: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface Ticket extends TicketSummary {
  comments: Comment[];
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface ErrorDetail {
  field: string;
  message: string;
}

export interface ApiErrorBody {
  code: string;
  message: string;
  details?: ErrorDetail[];
}
