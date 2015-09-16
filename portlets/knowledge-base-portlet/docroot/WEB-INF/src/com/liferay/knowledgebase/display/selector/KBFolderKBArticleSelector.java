/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.knowledgebase.display.selector;

import com.liferay.compat.portal.kernel.util.StringUtil;
import com.liferay.knowledgebase.model.KBArticle;
import com.liferay.knowledgebase.model.KBFolder;
import com.liferay.knowledgebase.model.KBFolderConstants;
import com.liferay.knowledgebase.model.impl.KBFolderImpl;
import com.liferay.knowledgebase.service.KBArticleLocalServiceUtil;
import com.liferay.knowledgebase.service.KBFolderLocalServiceUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.workflow.WorkflowConstants;

/**
 * @author Adolfo Pérez
 */
public class KBFolderKBArticleSelector implements KBArticleSelector {

	@Override
	public KBArticleSelection findByResourcePrimKey(
			long groupId, String preferredKBFolderUrlTitle,
			long ancestorResourcePrimKey, long resourcePrimKey)
		throws PortalException, SystemException {

		KBFolder ancestorKBFolder = _rootKBFolder;

		if (ancestorResourcePrimKey !=
				KBFolderConstants.DEFAULT_PARENT_FOLDER_ID) {

			ancestorKBFolder = KBFolderLocalServiceUtil.fetchKBFolder(
				ancestorResourcePrimKey);

			if (ancestorKBFolder == null) {
				return new KBArticleSelection(null, false);
			}
		}

		KBArticle kbArticle = KBArticleLocalServiceUtil.fetchLatestKBArticle(
			resourcePrimKey, WorkflowConstants.STATUS_APPROVED);

		if ((kbArticle == null) || !isDescendant(kbArticle, ancestorKBFolder)) {
			KBArticleSelection closestMatchingKBArticle =
				findClosestMatchingKBArticle(
					groupId, ancestorKBFolder, preferredKBFolderUrlTitle);

			closestMatchingKBArticle.setExactMatch(true);

			return closestMatchingKBArticle;
		}

		return new KBArticleSelection(kbArticle, true);
	}

	@Override
	public KBArticleSelection findByUrlTitle(
			long groupId, String preferredKBFolderUrlTitle,
			long ancestorResourcePrimKey, String kbFolderUrlTitle,
			String urlTitle)
		throws PortalException, SystemException {

		KBFolder ancestorKBFolder = _rootKBFolder;

		if (ancestorResourcePrimKey !=
				KBFolderConstants.DEFAULT_PARENT_FOLDER_ID) {

			ancestorKBFolder = KBFolderLocalServiceUtil.fetchKBFolder(
				ancestorResourcePrimKey);

			if (ancestorKBFolder == null) {
				return new KBArticleSelection(null, false);
			}
		}

		KBFolder kbFolder = _rootKBFolder;

		if (Validator.isNotNull(kbFolderUrlTitle)) {
			if (kbFolderUrlTitle.equals(ancestorKBFolder.getUrlTitle())) {
				kbFolder = ancestorKBFolder;
			}
			else {
				kbFolder = KBFolderLocalServiceUtil.fetchKBFolderByUrlTitle(
					groupId, ancestorKBFolder.getKbFolderId(),
					kbFolderUrlTitle);
			}
		}

		KBArticle kbArticle =
			KBArticleLocalServiceUtil.fetchKBArticleByUrlTitle(
				groupId, kbFolder.getKbFolderId(), urlTitle);

		if ((kbArticle == null) || !isDescendant(kbArticle, ancestorKBFolder)) {
			return findClosestMatchingKBArticle(
				groupId, ancestorKBFolder, preferredKBFolderUrlTitle,
				kbFolderUrlTitle, urlTitle);
		}

		return new KBArticleSelection(kbArticle, true);
	}

	protected KBArticleSelection findClosestMatchingKBArticle(
			long groupId, KBFolder ancestorKBFolder,
			String preferredKBFolderUrlTitle)
		throws PortalException, SystemException {

		KBFolder kbFolder = null;

		int kbArticlesCount = KBArticleLocalServiceUtil.getKBArticlesCount(
			groupId, ancestorKBFolder.getKbFolderId(),
			WorkflowConstants.STATUS_APPROVED);

		if (Validator.isNotNull(preferredKBFolderUrlTitle) &&
			(kbArticlesCount == 0)) {

			kbFolder = KBFolderLocalServiceUtil.fetchKBFolderByUrlTitle(
				groupId, ancestorKBFolder.getKbFolderId(),
				preferredKBFolderUrlTitle);
		}

		if ((kbFolder == null) && (kbArticlesCount == 0)) {
			kbFolder = KBFolderLocalServiceUtil.fetchFirstChildKBFolder(
				groupId, ancestorKBFolder.getKbFolderId());
		}

		if (kbFolder == null) {
			kbFolder = ancestorKBFolder;
		}

		KBArticle kbArticle =
			KBArticleLocalServiceUtil.fetchFirstChildKBArticle(
				groupId, kbFolder.getKbFolderId());

		return new KBArticleSelection(kbArticle, false);
	}

	protected KBArticleSelection findClosestMatchingKBArticle(
			long groupId, KBFolder ancestorKBFolder,
			String preferredKBFolderUrlTitle, String kbFolderUrlTitle,
			String urlTitle)
		throws PortalException, SystemException {

		KBFolder kbFolder = getCandidateKBFolder(
			groupId, preferredKBFolderUrlTitle, ancestorKBFolder,
			kbFolderUrlTitle);

		KBArticle kbArticle =
			KBArticleLocalServiceUtil.fetchKBArticleByUrlTitle(
				groupId, kbFolder.getKbFolderId(), urlTitle);

		if (kbArticle != null) {
			return new KBArticleSelection(kbArticle, false);
		}

		kbArticle = KBArticleLocalServiceUtil.fetchFirstChildKBArticle(
			groupId, kbFolder.getKbFolderId());

		String[] keywords = StringUtil.split(urlTitle, '-');

		return new KBArticleSelection(kbArticle, keywords);
	}

	protected KBFolder getCandidateKBFolder(
			long groupId, String preferredKBFolderUrlTitle,
			KBFolder ancestorKBFolder, String kbFolderUrlTitle)
		throws PortalException, SystemException {

		KBFolder kbFolder = null;

		if (Validator.isNotNull(kbFolderUrlTitle)) {
			kbFolder = KBFolderLocalServiceUtil.fetchKBFolderByUrlTitle(
				groupId, ancestorKBFolder.getKbFolderId(), kbFolderUrlTitle);
		}

		if ((kbFolder == null) &&
			Validator.isNotNull(preferredKBFolderUrlTitle)) {

			kbFolder = KBFolderLocalServiceUtil.fetchKBFolderByUrlTitle(
				groupId, ancestorKBFolder.getKbFolderId(),
				preferredKBFolderUrlTitle);
		}

		int kbArticlesCount = KBArticleLocalServiceUtil.getKBArticlesCount(
			groupId, ancestorKBFolder.getKbFolderId(),
			WorkflowConstants.STATUS_APPROVED);

		if ((kbFolder == null) && (kbArticlesCount == 0)) {
			kbFolder = KBFolderLocalServiceUtil.fetchFirstChildKBFolder(
				groupId, ancestorKBFolder.getKbFolderId());
		}

		if (kbFolder == null) {
			return ancestorKBFolder;
		}

		return kbFolder;
	}

	protected boolean isDescendant(
			KBArticle kbArticle, KBFolder ancestorKBFolder)
		throws PortalException, SystemException {

		if (ancestorKBFolder.getKbFolderId() ==
				KBFolderConstants.DEFAULT_PARENT_FOLDER_ID) {

			return true;
		}

		KBFolder parentKBFolder = KBFolderLocalServiceUtil.fetchKBFolder(
			kbArticle.getKbFolderId());

		while ((parentKBFolder != null) &&
			   !parentKBFolder.equals(ancestorKBFolder)) {

			parentKBFolder = KBFolderLocalServiceUtil.fetchKBFolder(
				parentKBFolder.getParentKBFolderId());
		}

		if (parentKBFolder != null) {
			return true;
		}

		return false;
	}

	private static final KBFolder _rootKBFolder;

	static {
		_rootKBFolder = new KBFolderImpl();

		_rootKBFolder.setKbFolderId(KBFolderConstants.DEFAULT_PARENT_FOLDER_ID);
	}

}