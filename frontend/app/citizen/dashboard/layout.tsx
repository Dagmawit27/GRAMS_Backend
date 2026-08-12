import { SidebarProvider, SidebarInset } from "@/components/ui/sidebar"
import { CitizenSidebarWrapper } from "./sidebar-wrapper"

export default function CitizenDashboardLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <SidebarProvider>
      <CitizenSidebarWrapper />
      <SidebarInset>{children}</SidebarInset>
    </SidebarProvider>
  )
}
