package org.tourism.instructors.api.catalog;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.tourism.instructors.api.catalog.dto.GradeDTO;
import org.tourism.instructors.application.catalog.CatalogService;

import static org.tourism.instructors.api.util.CommonAttributes.SUCCESS_MESSAGE_ATTRIBUTE;

@Controller
@RequestMapping("/catalog/grades")
public class GradeController {

    private static final String REDIRECT_URL = "redirect:/catalog/grades";
    private static final String GRADES_ATTRIBUTE = "grades";
    private static final String GRADE_ATTRIBUTE = "grade";

    private final CatalogService catalogService;

    public GradeController (CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    public String listGrades(@RequestParam(required = false, defaultValue = "false") boolean showInactive,
                             Model model) {
        if (showInactive) {
            model.addAttribute(GRADES_ATTRIBUTE, catalogService.findAllGrades());
        } else {
            model.addAttribute(GRADES_ATTRIBUTE, catalogService.findActiveGrades());
        }
        model.addAttribute("showInactive", showInactive);
        return "catalog/grades/list";
    }

    @GetMapping("/{id}")
    public String viewGrade(@PathVariable int id, Model model) {
        model.addAttribute(GRADE_ATTRIBUTE, catalogService.findGradeById(id));
        return "catalog/grades/view";
    }

    @GetMapping("/{id}/edit")
    public String editGrade(@PathVariable int id, Model model) {
        model.addAttribute(GRADE_ATTRIBUTE, catalogService.findGradeById(id));
        return "catalog/grades/edit";
    }

    @PostMapping("/{id}/edit")
    public String updateGrade(@PathVariable int id,
                              @ModelAttribute GradeDTO grade,
                              RedirectAttributes redirectAttributes) {
        grade.setId(id);
        catalogService.saveGrade(grade);
        redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE_ATTRIBUTE, "Звание успешно обновлено");
        return REDIRECT_URL;
    }

    @GetMapping("/new")
    public String newGradeForm(Model model) {
        model.addAttribute(GRADE_ATTRIBUTE, new GradeDTO());
        return "catalog/grades/edit";
    }

    @PostMapping("/new")
    public String createGrade(@ModelAttribute GradeDTO gradeDTO,
                              RedirectAttributes redirectAttributes) {
        catalogService.saveGrade(gradeDTO);
        redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE_ATTRIBUTE, "Звание успешно создано");
        return REDIRECT_URL;
    }

    @PostMapping("/{id}/delete")
    public String deleteGrade(@PathVariable int id,
                              RedirectAttributes redirectAttributes) {
        catalogService.deleteGrade(id);
        redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE_ATTRIBUTE, "Звание успешно удалено");
        return REDIRECT_URL;
    }
}
