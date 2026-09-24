import { afterEach, describe, expect, it, vi } from "vitest";
import { ApiError, listTickets } from "@/lib/api";

describe("API client", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("surfaces message and field details from a 400 response", async () => {
    const errorBody = {
      code: "VALIDATION_FAILED",
      message: "Request validation failed",
      details: [{ field: "title", message: "must not be blank" }],
    };
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        new Response(JSON.stringify(errorBody), {
          status: 400,
          headers: { "Content-Type": "application/json" },
        }),
      ),
    );

    const error = await listTickets(new URLSearchParams()).catch(
      (reason: unknown) => reason,
    );

    expect(error).toBeInstanceOf(ApiError);
    expect(error).toMatchObject({
      status: 400,
      body: errorBody,
    });
  });

  it("returns a controlled message when an error body is not JSON", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(new Response("Bad gateway", { status: 502 })),
    );

    await expect(listTickets(new URLSearchParams())).rejects.toMatchObject({
      status: 502,
      body: {
        code: "REQUEST_FAILED",
        message: "Request failed with status 502",
        details: [],
      },
    });
  });
});
