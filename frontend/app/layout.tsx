import type { Metadata } from "next";
import Link from "next/link";
import "./globals.css";

export const metadata: Metadata = {
  title: "Support Tickets",
  description: "Support ticket management system",
};

export default function RootLayout({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="en">
      <body>
        <a className="skip-link" href="#main-content">
          Skip to main content
        </a>
        <header className="app-header">
          <div className="header-inner">
            <Link className="brand" href="/">
              <span className="brand-mark">T</span>
              <span>
                <strong>Ticket Desk</strong>
                <small>Support workspace</small>
              </span>
            </Link>
          </div>
        </header>
        <main id="main-content">{children}</main>
      </body>
    </html>
  );
}
