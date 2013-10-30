/*******************************************************************************
 * Copyright (c) 2008, 2013
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Javier Canovas (javier.canovas@inria.fr) 
 *******************************************************************************/

package fr.inria.atlanmod.json.web;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EcorePackage;
import org.emftools.emf2gv.graphdesc.GraphdescPackage;
import org.emftools.emf2gv.processor.core.StandaloneProcessor;

import sun.misc.BASE64Encoder;
import fr.inria.atlanmod.JsonStandaloneSetup;

/**
 * Abstract class to factor some common behavior
 * 
 * @author Javier Canovas (javier.canovas@inria.fr)
 *
 */
public abstract class AbstractJsonDiscoverer extends HttpServlet {
	private static final long serialVersionUID = 69L;
	
	// The main path to the working dir (needed for generating the pictures)
	public static File workingDir = null;
	
	// The path to the Graphviz DOT execitable (needed for generating the pictures)
	public static String dotExePath = null;
	
	// The jsonParam used for discovery (where the json code is stored)
	static String jsonParam = null;
	
	Properties properties = null;

    @Override
    public void init() throws ServletException {
    	super.init();

    	JsonStandaloneSetup.doSetup();
    	
    	// Getting configuration
    	String workingDirString = null;
    	try {
        	properties = new Properties();
			properties.load(getServletContext().getResourceAsStream("/WEB-INF/config.properties"));
			workingDirString = properties.getProperty("workingDir");
			dotExePath = properties.getProperty("dotExePath");
			jsonParam = properties.getProperty("JsonParameter");
		} catch (IOException e) {
			throw new ServletException("Discover servlet could not find the configuration");
		}
    	
    	// We need a File (not a String)
		workingDir = new File(workingDirString);
		if(!workingDir.isDirectory()) throw new ServletException("The working dir does not exist");
    }
    

	/**
	 * Encodes a JPG picture into the BASE64 format
	 * 
	 * @param imagePath
	 * @return
	 * @throws IOException
	 */
	String encodeToString(File imagePath) throws IOException {
		BufferedImage image = ImageIO.read(imagePath);
		
	    String imageString = null;
	    ByteArrayOutputStream bos = new ByteArrayOutputStream();

	    try {
	        ImageIO.write(image, "JPG", bos);
	        byte[] imageBytes = bos.toByteArray();

	        BASE64Encoder encoder = new BASE64Encoder();
	        imageString = encoder.encode(imageBytes);

	        bos.close();
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	    return imageString;
	}
	
	/**
	 * Draws a model into a picture. To avoid file access problems, an unique id has to be 
	 * provided. A new directory using such id will be created.
	 * 
	 * @param elements Elements to be drawn
	 * @param uniqueId Id of the process asking for the generation 
	 * @return
	 * @throws ServletException
	 */
	File drawModel(List<EObject> elements, String uniqueId) throws ServletException {
		EcorePackage.eINSTANCE.eClass();
		GraphdescPackage.eINSTANCE.eClass();
		
		File uniqueWorkingDir = new File(workingDir.getAbsolutePath() + File.separator + uniqueId);
		if(!uniqueWorkingDir.isDirectory()) throw new ServletException("The working dir does not exist");
		
		File resultPath;
		try {
			resultPath = File.createTempFile("temp", ".jpg", uniqueWorkingDir);
		} catch (IOException e1) {
			throw new ServletException("Not possible to access to temp dir");
		}
		
		try {
			StandaloneProcessor.process(elements, null, uniqueWorkingDir, resultPath.getAbsolutePath(), null, null, dotExePath, true, false, "UTF-8", null, null, null);
		} catch (CoreException e) {
			e.printStackTrace();
			throw new ServletException("Not possible to generate the image");
		}
		
		return resultPath;
	}
}