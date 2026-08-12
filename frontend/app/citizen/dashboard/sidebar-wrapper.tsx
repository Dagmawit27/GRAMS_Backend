"use client"

import { useEffect, useState } from "react"
import { useRouter } from "next/navigation"
import { getSession, clearSession } from "@/lib/api"
import { CitizenSidebar } from "@/components/citizen-sidebar"

export function CitizenSidebarWrapper() {
  const router = useRouter()
  const [userName, setUserName] = useState<string>("")

  useEffect(() => {
    const session = getSession()
    if (session) {
      setUserName(`${session.user.firstName} ${session.user.lastName}`)
    }
  }, [])

  function handleLogout() {
    clearSession()
    router.push("/")
  }

  return <CitizenSidebar userName={userName} onLogout={handleLogout} />
}
