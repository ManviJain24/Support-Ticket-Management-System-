import { Suspense } from "react";
import TicketList from "@/components/TicketList";

export default function HomePage() {
  return (
    <Suspense fallback={<p>Loading tickets…</p>}>
      <TicketList />
    </Suspense>
  );
}
