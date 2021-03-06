/*******************************************************************************
 * Copyright (c) 2012 Julien Boulay - Ekito - www.ekito.fr.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Julien Boulay - Ekito - www.ekito.fr - initial API and implementation
 ******************************************************************************/
package org.talend.designer.cmis.manager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.talend.core.model.process.IElementParameter;
import org.talend.core.model.process.IExternalNode;
import org.talend.core.model.utils.TalendTextUtils;
import org.talend.designer.cmis.manager.impl.SessionManager;
import org.talend.designer.cmis.model.PropertyDefinitionModel;
import org.talend.designer.cmis.model.TypeDefinitionModel;

/**
 * This class holds the model and UI managers on behalf of the component.
 * 
 * @author Julien Boulay - Ekito - www.ekito.fr
 * 
 */
public class DefaultTypeDefinitionManagerImpl implements TypeDefinitionManager{

	public static final String PARAM_OBJECT_TYPE = "OBJECT_TYPE";

	public static final String PARAM_BASE_TYPE_ID = "BASE_TYPE_ID";

	// property mapping and additional items
	public static final String PARAM_PROPERTY_MAPPING = "PROPERTY_MAPPING";

	public static final String PARAM_OBJECT_TYPE_ID = "OBJECT_TYPE_ID";

	public static final String PARAM_ITEM_ID = "ID";

	public static final String PARAM_ITEM_TYPE = "TYPE";

	public static final String PARAM_ITEM_DEFAULT = "DEFAULT";

	private IExternalNode component;

	private SessionManager sessionManager;

	private PropertyDefinitionFilter propertyDefinitionFilter;

	public void setPropertyDefinitionFilter(
			PropertyDefinitionFilter propertyDefinitionFilter) {
		this.propertyDefinitionFilter = propertyDefinitionFilter;
	}

	// data model
	private List<TypeDefinitionModel> availableTypeDefinitionModel = new ArrayList<TypeDefinitionModel>();
	private List<PropertyDefinitionModel> availablePropertyDefinitions;
	private Map<String, PropertyDefinitionModel> availablePropertyDefinitionsMap;

	private TypeDefinitionModel selectedTypeDefinitionModel;
	private Map<String, PropertyDefinitionModel> selectedPropertyDefinitions = new HashMap<String, PropertyDefinitionModel>();

	public DefaultTypeDefinitionManagerImpl(IExternalNode component, SessionManager sessionManager) {
		this.component = component;
		this.sessionManager = sessionManager;
	}

	/**
	 * @return the object types available in the cmis database
	 */
	public List<TypeDefinitionModel> getAvailableTypeDefinition() {
		return availableTypeDefinitionModel;
	}

	public List<PropertyDefinitionModel> getAvailablePropertyDefinitions() {
		return availablePropertyDefinitions;
	}

	public Map<String, PropertyDefinitionModel> getAvailablePropertyDefinitionsMap() {
		return availablePropertyDefinitionsMap;
	}

	/**
	 * @param selectedTypeDefinitionModel
	 *            the selected object type
	 */
	public void setSelectedTypeDefinitionModel(
			TypeDefinitionModel selectedObjectTypeNode) {
		this.selectedTypeDefinitionModel = selectedObjectTypeNode;

		availablePropertyDefinitions = selectedObjectTypeNode.getPropertyDefinitions();

		if (propertyDefinitionFilter != null)
		{
			ArrayList<PropertyDefinitionModel> selectablePropertyDefinition = new ArrayList<PropertyDefinitionModel>();

			for (PropertyDefinitionModel propertyDefinitionModel : availablePropertyDefinitions) {

				if (propertyDefinitionFilter.isSelectable(propertyDefinitionModel))
				{
					selectablePropertyDefinition.add(propertyDefinitionModel);	
				}
			}
			availablePropertyDefinitions = selectablePropertyDefinition;
		}

		availablePropertyDefinitionsMap = new HashMap<String, PropertyDefinitionModel>();

		for (PropertyDefinitionModel propertyDefinitionModel : availablePropertyDefinitions) {

			availablePropertyDefinitionsMap.put(propertyDefinitionModel.getId(), propertyDefinitionModel);
		}

	}

	/**
	 * @return the selected TypeDefinition
	 */
	public TypeDefinitionModel getSelectedTypeDefinition() {
		return selectedTypeDefinitionModel;
	}

	public void addSelectedPropertyDefinition(
			PropertyDefinitionModel propertyDefinition) {
		selectedPropertyDefinitions.put(propertyDefinition.getId(),
				propertyDefinition);
	}

	public void removeSelectedPropertyDefinition(
			PropertyDefinitionModel propertyDefinitionModel) {
		selectedPropertyDefinitions.remove(propertyDefinitionModel.getId());
	}

	public void clearSelectedPropertyDefinition() {
		selectedPropertyDefinitions = new HashMap<String, PropertyDefinitionModel>();
	}

	public Map<String, PropertyDefinitionModel> getSelectedPropertyDefinitions() {
		return selectedPropertyDefinitions;
	}

	/**
	 * Clears the model
	 */
	public void clear() {
		selectedTypeDefinitionModel = null;
		selectedPropertyDefinitions = new HashMap<String, PropertyDefinitionModel>();
		availableTypeDefinitionModel = new ArrayList<TypeDefinitionModel>();
	}

	/**
	 * Loads the model of the component
	 * 
	 */
	public void load() {
		this.clear();

		String selectedBaseTypeId = (String) getComponent()
				.getElementParameter(PARAM_BASE_TYPE_ID).getValue();

		// load all the TypeDefinition from the CMIS server.
		availableTypeDefinitionModel = sessionManager.getTypeDefinitionModels(selectedBaseTypeId);

		// Get the selected ObjectType (hidden field)
		String selectedTypeDefinition = (String) getComponent()
				.getElementParameter(PARAM_OBJECT_TYPE).getValue();

		//If ObjectTypeId (displayed field) exists, get the slectedTypeDefinition from here 
		IElementParameter objectTypeIdElemParam = getComponent().getElementParameter(PARAM_OBJECT_TYPE_ID);
		if (objectTypeIdElemParam != null)
		{
			String objectTypeId = (String) objectTypeIdElemParam.getValue();
			
			if (objectTypeId != null && !objectTypeId.equals(""))
			{
				selectedTypeDefinition = objectTypeId;
			}
		}

		selectedTypeDefinition = TalendTextUtils.removeQuotes(selectedTypeDefinition);

		// Create a real model for the TreeViewer for a better management of the
		// tree nodes
		for (Iterator<TypeDefinitionModel> iterator = availableTypeDefinitionModel
				.iterator(); iterator.hasNext();) {
			TypeDefinitionModel availableTypeDefinition = (TypeDefinitionModel) iterator.next();

			TypeDefinitionModel foundTypeDefinition = availableTypeDefinition
					.findById(selectedTypeDefinition);

			if (foundTypeDefinition != null)
				setSelectedTypeDefinitionModel(foundTypeDefinition);

		}
		//Select the first node if any selected
		if (getSelectedTypeDefinition() == null)
		{
			setSelectedTypeDefinitionModel(availableTypeDefinitionModel.get(0));
		}

		// Get the selected object properties
		this.loadMetadatas(PARAM_PROPERTY_MAPPING);

	}

	/**
	 * Saves the model to the component
	 */
	public void save() {

		component.getElementParameter(PARAM_OBJECT_TYPE).setValue(
				selectedTypeDefinitionModel.getObjectTypeId());

		IElementParameter objectTypeIdElemParam = component.getElementParameter(PARAM_OBJECT_TYPE_ID);
		
		if (objectTypeIdElemParam != null)
		{
			String objectTypeId = selectedTypeDefinitionModel.getObjectTypeId();
			objectTypeIdElemParam.setValue(TalendTextUtils.addQuotes(objectTypeId));
		}

		component.getElementParameter(PARAM_BASE_TYPE_ID).setValue(
				selectedTypeDefinitionModel.getBaseTypeId());

		// Save the available metadatas;
		this.saveMetadatas(PARAM_PROPERTY_MAPPING);

	}

	/**
	 * Load the metadatas in the model manager
	 * 
	 * @param paramPropertyMapping
	 */
	@SuppressWarnings("unchecked")
	private void loadMetadatas(String metadataMappingParamName) {

		List<Map<String, String>> metadataMappingTable = (List<Map<String, String>>) component
				.getElementParameter(metadataMappingParamName).getValue();

		Map<String, PropertyDefinitionModel> propertyDefinitionMap = getAvailablePropertyDefinitionsMap();

		for (Map<String, String> metadataMappingRow : new ArrayList<Map<String, String>>(
				metadataMappingTable)) {
			String propertyId = metadataMappingRow.get(PARAM_ITEM_ID);

			PropertyDefinitionModel propertyDefinition = propertyDefinitionMap.get(propertyId);
			selectedPropertyDefinitions.put(propertyId, propertyDefinition);

		}
	}

	/**
	 * Saves the given metadata to the given map
	 * 
	 * @param metadataMappingParamName
	 * @param objectTypeId
	 */
	@SuppressWarnings("unchecked")
	private void saveMetadatas(String metadataMappingParamName) {
		List<Map<String, String>> metadataMappingTable = (List<Map<String, String>>) component
				.getElementParameter(metadataMappingParamName).getValue();

		Map<String, PropertyDefinitionModel> remainingPropertyDefinitions = new HashMap<String, PropertyDefinitionModel>(
				selectedPropertyDefinitions);

		// handling previously know metadata ; using a copy of their list so
		// we're able do removes in it
		for (Map<String, String> metadataMappingRow : new ArrayList<Map<String, String>>(
				metadataMappingTable)) {
			String propertyId = metadataMappingRow.get(PARAM_ITEM_ID);


			// trying to find the corresponding existing metadata...
			PropertyDefinitionModel propertyDef = selectedPropertyDefinitions
					.get(propertyId);

			if (propertyDef == null) {
				// has been removed ; let's remove it from the parameter
				metadataMappingTable.remove(metadataMappingRow);
				remainingPropertyDefinitions.remove(propertyId);
				continue;
			}else
			{
				//Ensure that the objectTypeId is up-to-date
				String objectTypeId = propertyDef.getObjectTypeId();
				metadataMappingRow.put(PARAM_OBJECT_TYPE_ID, objectTypeId );
			}

			remainingPropertyDefinitions.remove(propertyId);
		}

		// let's add the remaining metadata as new rows
		for (PropertyDefinitionModel propertyDefinition : remainingPropertyDefinitions
				.values()) {
			Map<String, String> metadataMappingRow = new HashMap<String, String>();
			fillMetadataMappingRow(metadataMappingRow, propertyDefinition,
					metadataMappingParamName);
			metadataMappingTable.add(metadataMappingRow);
		}

		// Sort the mapping table
		Collections.sort(metadataMappingTable,
				new Comparator<Map<String, String>>() {
			public int compare(Map<String, String> o1,
					Map<String, String> o2) {
				String paramItemId1 = o1.get(PARAM_ITEM_ID);
				paramItemId1 = paramItemId1 != null ? paramItemId1 : "";
				String paramItemId2 = o2.get(PARAM_ITEM_ID);
				paramItemId2 = paramItemId2 != null ? paramItemId2 : "";
				return paramItemId1.compareTo(
						paramItemId2);
			};
		});
	}

	/**
	 * Fills a (parameter) metadata row with metadata info
	 * 
	 * @param metadataMappingRow
	 * @param propertyDef
	 * @param metadataMappingParamName
	 */
	private void fillMetadataMappingRow(Map<String, String> metadataMappingRow,
			PropertyDefinitionModel propertyDef, String metadataMappingParamName) {
		if (PARAM_PROPERTY_MAPPING.equals(metadataMappingParamName)) {
			// case properties :
			fillPropertyMappingRow(metadataMappingRow, propertyDef);
		}
	}

	/**
	 * Fills a (parameter) property metadata row with property info
	 * 
	 * @param metadataMappingRow
	 * @param propertyDef
	 */
	private void fillPropertyMappingRow(
			Map<String, String> metadataMappingRow,
			PropertyDefinitionModel propertyDef) {

		if (metadataMappingRow != null
				&& propertyDef != null)
		{
			String objectTypeId = getSelectedTypeDefinition().getObjectTypeId();
			metadataMappingRow.put(PARAM_OBJECT_TYPE_ID, objectTypeId );
			metadataMappingRow.put(PARAM_ITEM_ID, propertyDef.getId());
			//			metadataMappingRow.put(PARAM_ITEM_TYPE, propertyDef.getPropertyType());
			//			metadataMappingRow.put(PARAM_ITEM_DEFAULT,	propertyDef.getDefaultValue());
		}
	}
	public IExternalNode getComponent() {
		return component;
	}

}
