# Implementation Plan — B.I.H.

## Goal
A private-first AI assistant for a single Android user: memory, routines, reminders, paperwork help, by text and voice.

## Principles
- Low cognitive load: short replies, one step at a time
- Confirm before saving anything important
- Privacy by default, explicit consent for cloud LLMs
- Emergency path always available

## Phases
- **Phase 0**: scaffolding, API health check, SQLite schema, privacy doc
- **Phase 1**: chat endpoint, notes, reminders, routines
- **Phase 2**: Android app (Chat, Today, Routines, Notes, Settings), notifications
- **Phase 3**: voice in/out, paperwork wizard, export
- **Phase 4**: encryption at rest, data export/delete, local model option

## Definition of done (MVP)
Chat by text, save/find notes, reminders with notifications, routine checklists, voice, and one paperwork flow with export.
