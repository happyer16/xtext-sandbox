/*******************************************************************************
 * Copyright (c) 2012 Michael Vorburger (http://www.vorburger.ch).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package ch.vorburger.xtext.databinding.internal.sourceadapt;

import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.util.concurrent.IUnitOfWork;


/**
 * SourceAccessor based on IXtextResourceReadWriteAccess
 * (IReadAccess<XtextResource> & IWriteAccess<XtextResource>),
 * normally the XtextDocument.
 * 
 * <p>Notification Adapters are registered on the Resource.</p>
 * 
 * @author Michael Vorburger
 */
public class XTextDocumentSourceAccessor implements SourceAccessor {

	private final IXtextResourceReadWriteAccess access;
	private final String uriFragment;
	
	public XTextDocumentSourceAccessor(IXtextResourceReadWriteAccess access) {
		this.access = access;
		this.uriFragment = "/"; // URI Fragment for EObject at the root of the Resource contents
	}
	
	public XTextDocumentSourceAccessor(XTextDocumentSourceAccessor parent, EObject eObject) {
		super();
		this.access = parent.access;
		Resource resource = eObject.eResource();
		if (resource != null) {
			uriFragment = resource.getURIFragment(eObject);
			if (uriFragment == null)
				throw new IllegalArgumentException("Resource returned null for URI Fragment for EObject: " + eObject.toString());
		} else {
			throw new IllegalArgumentException("EObject has no eResource to get URIFragment from: " + eObject.toString());
		}
	}
	
	@Override
	public void eSet(final EStructuralFeature feature, final Object value) {
	    access.modify(new IUnitOfWork.Void<XtextResource>() {
	    	@Override public void process(XtextResource resource) throws Exception {
	    		// TODO HIGH Handling (via TDD) if it doesn't exist yet! Ideally, don't throw an error, but create it on-the-fly...
	    		EObject eObject = resource.getEObject(uriFragment);
				if (eObject != null)
					eObject.eSet(feature, value);
				else
		    		throw new IllegalStateException("Uh uh, no content in Resource - why you asking to set it, calling code? Feature: " + feature +  ", URI Fragment: " + uriFragment + ", Resource: " + resource);
	    	};
		});
	}

	@Override
	public Object eGet(final EStructuralFeature feature) {
		return access.readOnly(new IUnitOfWork<Object, XtextResource>() {
			@Override public Object exec(XtextResource resource) throws Exception {
				EObject eObject = resource.getEObject(uriFragment);
				if (eObject != null)
					return eObject.eGet(feature);
				else
					return null;
			}
		});
	}

	@Override
	public void addAdapter(Adapter adapter) {
		getResource().eAdapters().add(adapter);
	}

	@Override
	public void removeAdapter(Adapter adapter) {
		getResource().eAdapters().remove(adapter);
	}
	
	protected Resource getResource() {
		return access.readOnly(new IUnitOfWork<Resource, XtextResource>() {
			@Override public Resource exec(XtextResource state) throws Exception {
	    		return state;
			}
		});
	}

}
