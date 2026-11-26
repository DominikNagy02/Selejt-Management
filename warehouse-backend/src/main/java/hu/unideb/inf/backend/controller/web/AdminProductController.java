package hu.unideb.inf.backend.controller.web;

import hu.unideb.inf.backend.service.facade.AdminViewFacade;
import hu.unideb.inf.core.dto.ProductRequest;
import hu.unideb.inf.core.dto.ProductResponse;
import hu.unideb.inf.core.enums.ZoneType;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("activeMenu", "products");
        model.addAttribute("productRequest", new ProductRequest(
                "",
                "",
                "",
                "",
                null,
                null,
                false,
                ZoneType.NORMAL,
                0,
                true
        ));
        model.addAttribute("zoneTypes", ZoneType.values());
        model.addAttribute("isEdit", false);
        model.addAttribute("productId", null);
        model.addAttribute("currentUsername", adminViewFacade.getCurrentUsername());
        model.addAttribute("currentUserRole", adminViewFacade.getCurrentUserRole());
        return "admin/products/form";
    }

    @PostMapping
    public String createProduct(
            @Valid @ModelAttribute("productRequest") ProductRequest productRequest,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("activeMenu", "products");
            model.addAttribute("zoneTypes", ZoneType.values());
            model.addAttribute("isEdit", false);
            model.addAttribute("productId", null);
            model.addAttribute("currentUsername", adminViewFacade.getCurrentUsername());
            model.addAttribute("currentUserRole", adminViewFacade.getCurrentUserRole());
            return "admin/products/form";
        }
        adminViewFacade.createProduct(productRequest);
        redirectAttributes.addFlashAttribute("successMessage", "Termék sikeresen rögzítve!");
        return "redirect:/admin/products";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable("id") Long id, Model model) {
        ProductResponse product = adminViewFacade.getProductById(id);
        ProductRequest productRequest = new ProductRequest(
                product.sku(),
                product.barcode(),
                product.name(),
                product.description(),
                product.unitWeightKg(),
                product.unitVolumeM3(),
                product.requiresExpiry(),
                product.requiredZoneType(),
                product.minStockLevel(),
                product.active()
        );
        model.addAttribute("activeMenu", "products");
        model.addAttribute("productRequest", productRequest);
        model.addAttribute("zoneTypes", ZoneType.values());
        model.addAttribute("isEdit", true);
        model.addAttribute("productId", id);
        model.addAttribute("currentUsername", adminViewFacade.getCurrentUsername());
        model.addAttribute("currentUserRole", adminViewFacade.getCurrentUserRole());
        return "admin/products/form";
    }

    @PostMapping("/{id}")
    public String updateProduct(
            @PathVariable("id") Long id,
            @Valid @ModelAttribute("productRequest") ProductRequest productRequest,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("activeMenu", "products");
            model.addAttribute("zoneTypes", ZoneType.values());
            model.addAttribute("isEdit", true);
            model.addAttribute("productId", id);
            model.addAttribute("currentUsername", adminViewFacade.getCurrentUsername());
            model.addAttribute("currentUserRole", adminViewFacade.getCurrentUserRole());
            return "admin/products/form";
        }
        adminViewFacade.updateProduct(id, productRequest);
        redirectAttributes.addFlashAttribute("successMessage", "Termék sikeresen módosítva!");
        return "redirect:/admin/products";
    }
}
