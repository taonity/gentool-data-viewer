import { redirect } from 'next/navigation'
import { safeLocalPath } from '@/lib/auth'

export default async function LoginPage({ searchParams }: { searchParams: Promise<{ next?: string }> }) {
  const { next } = await searchParams
  redirect(safeLocalPath(next ?? null) ?? '/')
}
