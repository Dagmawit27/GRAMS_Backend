import { SidebarProvider, SidebarInset } from "@/components/ui/sidebar"
import { OfficerSidebar } from "./officer-sidebar"

export default function OfficerDashboardLayout({ children }: { children: React.ReactNode }) {
  return (
    <SidebarProvider>
      <OfficerSidebar />
      <SidebarInset>{children}</SidebarInset>
    </SidebarProvider>
  )
}
