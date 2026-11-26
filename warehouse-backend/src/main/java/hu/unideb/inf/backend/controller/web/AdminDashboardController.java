package hu.unideb.inf.backend.controller.web;

import hu.unideb.inf.backend.service.facade.AdminViewFacade;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminDashboardController {

    private final AdminViewFacade adminViewFacade;

    public AdminDashboardController(AdminViewFacade adminViewFacade) {
        this.adminViewFacade = adminViewFacade;
    }

    @GetMapping({"", "/", "/dashboard"})
    public String dashboard(Model model) {
        model.addAttribute("activeMenu", "dashboard");
        model.addAttribute("totalProducts", adminViewFacade.getTotalProducts());
        model.addAttribute("occupancyRate", adminViewFacade.getOccupancyRate());
        model.addAttribute("criticalStockCount", adminViewFacade.getCriticalStockCount());
        model.addAttribute("activeTasks", adminViewFacade.getActiveTasksCount());
        model.addAttribute("zoneLoads", adminViewFacade.getZoneLoads());
        model.addAttribute("stockAlerts", adminViewFacade.getStockAlerts());
        model.addAttribute("currentUsername", adminViewFacade.getCurrentUsername());
        model.addAttribute("currentUserRole", adminViewFacade.getCurrentUserRole());
        return "admin/dashboard";
    }
}
