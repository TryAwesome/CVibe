# Frontend Design & UX Specification

> **Note to OpenCode:** This document defines the visual architecture, component strategy, and user experience standards for the `/frontend` application. The stack is **Next.js (App Router)**, **React**, **Tailwind CSS**, and **Shadcn/UI**.

## 1. Design System & Aesthetics

*   **Framework**: Next.js 14+ (App Router).
*   **Styling**: Tailwind CSS.
*   **Component Library**: **Shadcn/UI** (Radix UI based).
*   **Iconography**: `lucide-react`.
*   **Theme**: **CVibe Black (Monochrome Minimalist)**.
    *   **Primary**: Black (`#18181B` / `zinc-950`).
    *   **Secondary**: Gray/White.
    *   **Accents**: Removed. The interface relies on contrast, whitespace, and "glassmorphism" effects.
    *   **Dark Mode**: Supported (System Default). In Dark Mode, Primary inverts to White.
*   **Typography**:
    *   **UI**: `Inter` (sans-serif).
    *   **Brand Headers**: `Zen Dots` (for "CVibe" logo).
    *   **Code/Editor**: `JetBrains Mono`.

## 2. Application Structure (Routes)

### 2.1 Public Zone
*   `/login` & `/register`:
    *   **Style**: Full-screen background image, centered glassmorphism card.
    *   **Interactions**: 3D Gravity Tilt effect on cards.
    *   **Auth**: Email/Password + Google OAuth.

### 2.2 User Dashboard (`/dashboard` Layout)
*   **Layout**: Fixed Left Sidebar + Top Header (Notification Bell) + Main Content Area.
*   **Navigation Order**:
    1.  **Dashboard** (`/dashboard`): Overview. Default view shows "Last Learning Path" if no module is selected.
    2.  **Interview** (`/interview`): AI Profile Builder Chat.
    3.  **Career Growth** (`/growth`): Gap Analysis & Roadmap.
    4.  **Resume Builder** (`/resume-builder`): LaTeX Editor.
    5.  **Job Recommendations** (`/jobs`): Daily crawled matches.
    6.  **Mock Interview** (`/mock-interview`): Stress simulation.
    7.  **Community** (`/community`): Discussion board.
*   **Settings** (`/settings`): Profile, Password (change), **AI Config** (API Key/Base URL/Model).

## 3. Core UX Patterns

### 3.1 Dashboard Logic
*   **Three-Card Nav**: Top of Dashboard has 3 clickable cards (Resume History, Profile DB, Matched Jobs).
*   **Resume History Card**: Upload PDF resumes; exported PDFs from Resume Builder are auto-added here.
*   **Profile DB Card**: Built from the last Interview; if none, derived from the most recent uploaded PDF resume.
*   **Matched Jobs Card**: Shows the last daily match snapshot, with "Why you match" highlights and external links.
*   **Expandable Details**: Clicking a card reveals details below it.
*   **Empty State**: If no card selected, displays the **Current Learning Path** timeline.

### 3.2 Interview (Profile Builder)
*   **Goal**: Build a complete profile with careful, exhaustive questioning.
*   **Coverage**: Ask about all resume-relevant areas; deep dive into work experience and research with detailed follow-ups.
*   **UI**: Chat-first flow optimized for long-form answers.

### 3.3 Resume Builder
*   **Wizard Step 1**: Select a system template or upload your own `.tex` + **Upload Job Description (Screenshot)**.
*   **Wizard Step 2**: AI Generation Animation (Scanning, Extracting, Writing) using HC requirements to pick, emphasize, and polish relevant experiences from the Profile DB.
*   **Wizard Step 3**: **Split Editor**.
    *   Left: LaTeX Source Code (Editable).
    *   Right: Real-time PDF Preview.
    *   Exported PDFs are added to Resume History.

### 3.4 Career Growth
*   **Input**: Text Input (Target Role) OR **Upload Image** (HC Requirement).
*   **Output**: Gap Analysis + Actionable Learning Path with concrete steps.

### 3.5 Mock Interview
*   **Tone**: High pressure, skeptical.
*   **Content**: Random Big Tech coding questions, "BaGuWen", resume deep dive, and business interview prompts.
*   **UI**: Full-screen Chat Interface.

### 3.6 Community
*   **Feed**: User posts (Text + Images).
*   **Interaction**: Comments and Likes.
*   **Notifications**: Global top-bar bell icon for interactions.

## 4. State Management

*   **Client State**: React Context for Auth, simple `useState` for local UI interactions.
*   **Mock Data**: Currently using comprehensive mock data for all modules to demonstrate flows.

## 5. Directory Structure

```text
/src
  /app
    /(auth)           # Login, Register
    /(dashboard)      # Layout with Sidebar + Header
      /dashboard      # Home
      /interview
      /growth
      /resume-builder
      /jobs
      /mock-interview
      /community
      /settings
  /components
    /ui               # Shadcn components
    /layout           # Sidebar, Header
    /dashboard        # Dashboard sub-views
```
