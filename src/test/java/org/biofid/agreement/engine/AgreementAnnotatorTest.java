package org.biofid.agreement.engine;

import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.DoubleArray;
import org.apache.uima.jcas.cas.LongArray;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.util.CasIOUtils;
import org.junit.jupiter.api.Test;
import org.texttechnologylab.annotation.AbstractNamedEntity;
import org.texttechnologylab.annotation.NamedEntity;
import org.texttechnologylab.iaa.Agreement;
import org.texttechnologylab.iaa.AgreementContainer;

import java.io.File;
import java.io.FileInputStream;

/**
 * Created on 28.01.2019.
 */
public class AgreementAnnotatorTest {
	@Test
	public void testAnnotatorAgreement() {
		try {
			String[] annotatorWhitelist = {"305236", "305235"};
			String[] annotatorBlacklist = {"0", "302904"};
			String xmiPath = "src/test/out/xmi/";
			
			JCas jCas = JCasFactory.createJCas();
			try (FileInputStream inputStream = FileUtils.openInputStream(new File(xmiPath + "3713524.xmi"))) {
				CasIOUtils.load(inputStream, null, jCas.getCas(), true);
				
				
				// Test parameters
				boolean filterFingerprinted = false;
				String[] annotationClasses = {NamedEntity.class.getName(), AbstractNamedEntity.class.getName()};
				
				AnalysisEngine annotatorEngine = AnalysisEngineFactory.createEngine(
						AgreementAnnotatorEngine.class,
						AgreementAnnotatorEngine.PARAM_ANNOTATION_CLASSES, annotationClasses,
						AgreementAnnotatorEngine.PARAM_MIN_VIEWS, 2,
//						AgreementAnnotatorEngine.PARAM_ANNOTATOR_LIST, annotatorWhitelist,
//						AgreementAnnotatorEngine.PARAM_ANNOTATOR_RELATION, AgreementAnnotatorEngine.WHITELIST,
						AgreementAnnotatorEngine.PARAM_ANNOTATOR_LIST, annotatorBlacklist,
						AgreementAnnotatorEngine.PARAM_ANNOTATOR_RELATION, AgreementAnnotatorEngine.BLACKLIST,
						AgreementAnnotatorEngine.PARAM_FILTER_FINGERPRINTED, filterFingerprinted,
						AgreementAnnotatorEngine.PARAM_AGREEMENT_MEASURE, AgreementAnnotatorEngine.KrippendorffAlphaAgreement,
						AgreementAnnotatorEngine.PARAM_SET_SELECTION_STRATEGY, SetSelectionStrategy.MAX,
						AgreementAnnotatorEngine.PARAM_PRINT_STATS, false
				);
				
				SimplePipeline.runPipeline(jCas, annotatorEngine);
				
				JCas iaa = jCas.getView("IAA");
				
				AgreementContainer agreementContainer = Lists.newArrayList(JCasUtil.select(iaa, AgreementContainer.class)).get(0);
				StringArray categoryNames = agreementContainer.getCategoryNames();
				DoubleArray categoryAgreementValues = agreementContainer.getCategoryAgreementValues();
				LongArray categoryCounts = agreementContainer.getCategoryCounts();
				
				System.out.println("Category\tAgreement");
				System.out.printf("Overall\t%f\n", agreementContainer.getOverallAgreementValue());
				for (int i = 0; i < categoryNames.size(); i++) {
					String category = categoryNames.get(i);
					Double value = categoryAgreementValues.get(i);
					Long count = categoryCounts.get(i);
					System.out.printf("%s\t%d\t%f\n", category, count, value);
				}
				System.out.println();
				
				System.out.println("Token\tAgreement");
				Lists.newArrayList(JCasUtil.select(iaa, Agreement.class)).subList(0, 100)
						.forEach(agreement -> System.out.printf("%s\t%f\n", agreement.getCoveredText(), agreement.getAgreementValue()));
				System.out.println();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("\nDone");
	}
}
