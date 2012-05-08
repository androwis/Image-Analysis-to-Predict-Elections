package elections.control;

import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;
import elections.model.UploadItem;
import elections.tools.CombinedKMeansActions;
import elections.tools.KMeansActions;

@Controller
public class ElectionController{


	@RequestMapping(method = RequestMethod.GET, value={"/training"})
	public ModelAndView training() {
		return buildPage(
				"index",
				"Exploring Histograms of Images",
				"the histogram",	
		"pages/training" +
		".jsp");
	}
	@RequestMapping(method = RequestMethod.GET, value={"/histograms"})
	public ModelAndView histograms() {
		return buildPage(
				"index",
				"Exploring Histograms of Images",
				"the histogram",	
		"pages/histograms.jsp");
	}
	@RequestMapping(method = RequestMethod.GET, value={"/histograms-hsv"})
	public ModelAndView histogramsHSV() {
		return buildPage(
				"index",
				"Exploring HSV Colors of Images",
				"HSV histogram ",	
		"pages/histograms-hsv.jsp");
	}
	
	// Image Processing API----------------------------------------------------------------------
	// ------------------------------------------------------------------------------------------
	@RequestMapping(method = RequestMethod.GET, value={"/upload","/image-segmentation"})
	public ModelAndView step1() {
		return buildPage(
				"index",
				"How to segment an image for processing.",
				"an image",	
		"pages/segmentation.jsp")
		.addObject("uploadItem", new UploadItem());
	}

	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(method = RequestMethod.POST, value = {"/upload","/image-segmentation"})
	public void create(UploadItem uploadItem, BindingResult result)
	{
		if (result.hasErrors())
		{
			for(ObjectError error : result.getAllErrors())
			{
				System.err.println("Error: " + error.getCode() +  " - " + error.getDefaultMessage());
			}
	        
		}
		// Some type of file processing...
		try {
			KMeansActions.SegmentImage(uploadItem.getFileData().getOriginalFilename(),ImageIO.read(uploadItem.getFileData().getInputStream()));
		} catch (Exception e) {
			System.err.println("woopsidasical...!@#!@$!");
		}
	}
	
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(method = RequestMethod.POST, value = {"/upload/{image}","/image-segmentation/{image}"})
	public void create(UploadItem uploadItem, BindingResult result, @PathVariable("image") String image)
	{
		try {
			KMeansActions.SegmentImage(image+".jpg",ImageIO.read(new File("src/main/resources/imgs/"+image+".jpg")));
		} catch (Exception e) {
			System.err.println(e.toString()+" || src/main/resources/imgs/"+image);
		}
		System.err.println("-------------------------------------------");
	}

	
	// Combined Image Processing API----------------------------------------------------------------------
	// ------------------------------------------------------------------------------------------
	@RequestMapping(method = RequestMethod.GET, value={"/combined-segmentation"})
	public ModelAndView combined() {
		return buildPage(
				"index",
				"Combined Image Analysis",
				"an image",	
		"pages/combined-segmentation.jsp")
		.addObject("uploadItem", new UploadItem());
	}

	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(method = RequestMethod.POST, value = {"combined-segmentation"})
	public void createCombined(UploadItem uploadItem, BindingResult result)
	{
		if (result.hasErrors())
		{
			for(ObjectError error : result.getAllErrors())
			{
				System.err.println("Error: " + error.getCode() +  " - " + error.getDefaultMessage());
			}
	        
		}
		// Some type of file processing...
		try {
			KMeansActions.SegmentImage(uploadItem.getFileData().getOriginalFilename(),ImageIO.read(uploadItem.getFileData().getInputStream()));
		} catch (Exception e) {
			System.err.println("woopsidasical...!@#!@$!");
		}
	}
	
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(method = RequestMethod.POST, value = {"/combined-segmentation/{image}"})
	public void createCombined(UploadItem uploadItem, BindingResult result, @PathVariable("image") String image)
	{
		try {
			CombinedKMeansActions.SegmentImage(image+".jpg",ImageIO.read(new File("src/main/resources/imgs/"+image+".jpg")),0,0,0,0);
		} catch (Exception e) {
			System.err.println(e.toString()+" || src/main/resources/imgs/"+image);
		}
		System.err.println("-------------------------------------------");
	}

	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(method = RequestMethod.POST, value = {"/combined-segmentation/{image}/{x}/{y}/{h}/{w}"})
	public void createCombinedWithFace(UploadItem uploadItem, BindingResult result, @PathVariable("image") String image,
			@PathVariable("x") int x,
			@PathVariable("y") int y,
			@PathVariable("w") int w,
			@PathVariable("h") int h)
	{
		System.err.println("-------------------------------------------");
		System.err.println("-------------------------------------------");
		try {
			BufferedImage b =ImageIO.read(new File("src/main/resources/imgs/romney-r-7.jpg"));
			CombinedKMeansActions.SegmentImage(image+".jpg",b,29,18,27,27);
		} catch (Exception e) {
			System.err.println(e.toString()+" || src/main/resources/imgs/"+image+ "/"+x+"/"+y+"/"+h+"/"+w);
		}
		System.err.println("-------------------------------------------");
		System.err.println("-------------------------------------------");
	}

	
	
	
	
	// --- helper function ----------------------------------------
	public static ModelAndView buildPage(String page, String title, String who, String include){
		ModelAndView temp = new ModelAndView(page)
		.addObject("title", title)
		.addObject("who", who)
		.addObject("include",include);
		return temp;
	}

	public ModelAndView addErrorMessage(String s){
		ModelAndView mv = ElectionController.buildPage(
				"index","Register w/ Scenedipity","login","pages/register.jsp");
		mv.addObject("message", s);
		mv.addObject("message-type","error");
		return mv;
	}

	@RequestMapping( value = "/favicon", method = RequestMethod.GET)
	public ModelAndView getFavicon() { return new ModelAndView();}

	// --- end helper functions----------------------------------


	// ------------------- Home Page ------------------------------
	// ------------------------------------------------------------
	@RequestMapping( value = {"/"}, method = RequestMethod.GET)
	public ModelAndView viewAll() {
		return buildPage(
				"index",
				"Snapshot of the 2012 Presidential Election",
				"US Election",	
		"pages/home.jsp");
	}

}
