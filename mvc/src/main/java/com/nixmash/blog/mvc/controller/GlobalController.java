package com.nixmash.blog.mvc.controller;

import com.nixmash.blog.jpa.common.ApplicationSettings;
import com.nixmash.blog.jpa.common.SiteOptions;
import com.nixmash.blog.jpa.exceptions.*;
import com.nixmash.blog.jpa.model.CurrentUser;
import com.nixmash.blog.mvc.components.WebUI;
import com.nixmash.blog.solr.exceptions.GeoLocationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.Authentication;
import org.springframework.social.connect.ConnectionData;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;

import static com.nixmash.blog.mvc.controller.GeneralController.HOME_VIEW;

@ControllerAdvice
public class GlobalController {

    private static final Logger logger = LoggerFactory.getLogger(GlobalController.class);

    protected static final String ERROR_CUSTOM_VIEW = "errors/custom";

    private static final String PRODUCT_MAP_VIEW = "products/map";
    private static final String LOCATION_ERROR_MESSAGE_KEY = "product.map.page.feedback.error";
    public static final String LOCATION_ERROR_ATTRIBUTE = "mappingError";
    public static final String SESSION_USER_CONNECTION = "MY_USER_CONNECTION";

    public static final String ERROR_PAGE_TITLE_ATTRIBUTE = "errortitle";
    public static final String ERROR_PAGE_MESSAGE_ATTRIBUTE = "errormessage";

    private final WebUI webUI;
    private final ApplicationSettings applicationSettings;
    private final SiteOptions siteOptions;

    @Autowired
    public GlobalController(SiteOptions siteOptions, WebUI webUI, ApplicationSettings applicationSettings) {
        this.siteOptions = siteOptions;
        this.webUI = webUI;
        this.applicationSettings = applicationSettings;
    }

    @ModelAttribute("currentUser")
    public CurrentUser getCurrentUser(Authentication authentication) {
        CurrentUser currentUser = null;
        if (authentication == null)
            return null;
        else {
            currentUser = (CurrentUser) authentication.getPrincipal();
        }
        return currentUser;
    }

    /**
     * Determines if the CurrentUser is "user".
     * For enabling or disabling certain user account functions
     *
     * @param authentication
     * @return TRUE is username=="user", otherwise FALSE
     */
    @ModelAttribute("isDemoUser")
    public Boolean isDemoUser(Authentication authentication) {
        Boolean isDemoUser = false;
        if (authentication != null) {
            if (authentication.getName().equals("user")) {
                isDemoUser = true;
            }
        }
        return isDemoUser;
    }

    @ModelAttribute("currentUserConnection")
    public ConnectionData getUserConnection(WebRequest request) {
        return (ConnectionData) request.getAttribute(SESSION_USER_CONNECTION,
                RequestAttributes.SCOPE_SESSION);
    }

    @ModelAttribute("appSettings")
    public ApplicationSettings getApplicationSettings() {
        return applicationSettings;
    }

    @ModelAttribute("siteOptions")
    public SiteOptions getSiteOptions() {
        return siteOptions;
    }

    @ModelAttribute("displayGoogleAnalytics")
    public Boolean displayGoogleAnalytics(Authentication authentication) {
        CurrentUser currentUser = this.getCurrentUser(authentication);
        Boolean displayAnalytics = siteOptions.getAddGoogleAnalytics();

        if (currentUser != null) {
            boolean isDisplayUser = !(currentUser.isAdmin() || currentUser.isPostUser());
            return displayAnalytics && isDisplayUser;
        } else
            return displayAnalytics;
    }

    @ExceptionHandler(PostCategoryNotSupportedException.class)
    public ModelAndView handleCategoryNotSupportedException(HttpServletRequest request) {
        ModelAndView mav = new ModelAndView();
        mav.addObject("category", (String) request.getAttribute("category"));
        String postName = (String) request.getAttribute("postName");
        String newurl = "/post/" + postName;
        mav.addObject("newurl", newurl);
        mav.setViewName("errors/category");
        return mav;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public String handleResourceNotFoundException() {
        return "errors/404";
    }

    @ExceptionHandler(ContactNotFoundException.class)
    public ModelAndView handleContactNotFoundException() {
        logger.debug("In ContactNotFound Exception Handler");

        ModelAndView mav = new ModelAndView();
        mav.addObject(ERROR_PAGE_TITLE_ATTRIBUTE, "Contact Missing in Action!");
        mav.addObject(ERROR_PAGE_MESSAGE_ATTRIBUTE, "We'll find the rascal, don't you worry");
        mav.setViewName(ERROR_CUSTOM_VIEW);
        return mav;
    }

    @ExceptionHandler(GeoLocationException.class)
    public ModelAndView handleGeoLocationException(HttpServletRequest request) {
        ModelAndView mav = new ModelAndView();
        String location = (String) request.getAttribute("location");
        String msg = webUI.getMessage(LOCATION_ERROR_MESSAGE_KEY, location);
        mav.addObject(LOCATION_ERROR_ATTRIBUTE, msg);
        mav.setViewName(PRODUCT_MAP_VIEW);
        return mav;
    }

    @ExceptionHandler(DisabledException.class)
    public ModelAndView handleDisabledUserException(String msg, HttpServletRequest request) {
        ModelAndView mav = new ModelAndView();
        mav.setViewName(HOME_VIEW);
        return mav;
    }

    @ExceptionHandler(DuplicatePostNameException.class)
    public ModelAndView handleDuplicatePostNameException(
            HttpServletRequest request) {
        ModelAndView mav = new ModelAndView();
        String postTitle = (String) request.getAttribute("postTitle");
        mav.addObject(ERROR_PAGE_TITLE_ATTRIBUTE, "Duplicate Post Name");
        mav.addObject(ERROR_PAGE_MESSAGE_ATTRIBUTE, String.format("\"%s\" exists.<br /> " +
                "Please rename your post title and try again.", postTitle));
        mav.setViewName(ERROR_CUSTOM_VIEW);
        return mav;
    }

    @ExceptionHandler(PostNotFoundException.class)
    public ModelAndView handlePostNotFoundException(
            HttpServletRequest request) {
        ModelAndView mav = new ModelAndView();
        mav.addObject(ERROR_PAGE_TITLE_ATTRIBUTE, "Post Not Found");
        mav.addObject(ERROR_PAGE_MESSAGE_ATTRIBUTE, "No post retrieved for your ID or Post Name");
        mav.setViewName(ERROR_CUSTOM_VIEW);
        return mav;
    }

}
