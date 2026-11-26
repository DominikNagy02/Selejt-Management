package hu.unideb.inf.backend.controller.web;

import hu.unideb.inf.backend.service.facade.AdminViewFacade;
import hu.unideb.inf.core.enums.ZoneType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/products")
public class AdminProductController {

    private final AdminViewFacade adminViewFacade;

    public AdminProductController(AdminViewFacade adminViewFacade) {
        this.adminViewFacade = adminViewFacade;
    }

    @GetMapping
    public String listProducts(
            @RequestParam(name = "query", required = false) String query,
            @RequestParam(name = "zoneType", required = false) ZoneType zoneType,
            Model model) {
        model.addAttribute("activeMenu", "products");
        model.addAttribute("products", adminViewFacade.getProducts(query, zoneType));
        model.addAttribute("zoneTypes", ZoneType.values());
        model.addAttribute("query", query != null ? query : "");
        model.addAttribute("selectedZoneType", zoneType);
        model.addAttribute("currentUsername", adminViewFacade.getCurrentUsername());
        model.addAttribute("currentUserRole", adminViewFacade.getCurrentUserRole());
        return "admin/products/list";
    }
}
