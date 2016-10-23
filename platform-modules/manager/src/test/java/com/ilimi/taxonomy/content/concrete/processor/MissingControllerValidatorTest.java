package com.ilimi.taxonomy.content.concrete.processor;

import java.io.File;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.ilimi.common.exception.ClientException;
import com.ilimi.taxonomy.content.common.BaseTest;
import com.ilimi.taxonomy.content.common.ContentErrorMessageConstants;
import com.ilimi.taxonomy.content.entity.Plugin;
import com.ilimi.taxonomy.content.util.ECRFConversionUtility;

public class MissingControllerValidatorTest extends BaseTest {

	final static File folder = new File("src/test/resources/Contents/Verbs");
	
	@Rule
	public ExpectedException exception = ExpectedException.none();

	@Test
	public void MissingAssetValidatorTest_01() {
			exception.expect(ClientException.class);
			exception.expectMessage(ContentErrorMessageConstants.INVALID_CWP_CONST_PARAM);
			
			ECRFConversionUtility fixture = new ECRFConversionUtility();
			String strContent = getFileString("Verbs_III/index.ecml");
			Plugin plugin = fixture.getECRF(strContent);
			PipelineRequestorClient.getPipeline("missingCtrlValidatorProcessor", folder.getPath(), "")
			.execute(plugin);
	}
	
	@Test
	public void MissingAssetValidatorTest_02() {
			exception.expect(ClientException.class);
			exception.expectMessage(ContentErrorMessageConstants.INVALID_CWP_CONST_PARAM);
			
			ECRFConversionUtility fixture = new ECRFConversionUtility();
			String strContent = getFileString("Verbs_III/index.ecml");
			Plugin plugin = fixture.getECRF(strContent);
			PipelineRequestorClient.getPipeline("missingCtrlValidatorProcessor", "", "")
			.execute(plugin);
	}
	
	@Test
	public void missingController() {
		exception.expect(ClientException.class);
		exception.expectMessage(ContentErrorMessageConstants.MISSING_CONTROLLER_FILES_ERROR);
		ECRFConversionUtility fixture = new ECRFConversionUtility();
		String strContent = getFileString("testAsset/index.ecml");
		Plugin plugin = fixture.getECRF(strContent);
		PipelineRequestorClient.getPipeline("missingCtrlValidatorProcessor", "src/test/resources/Contents/testAsset", "test_12")
				.execute(plugin);
	}

	@Test
	public void missingControllerOfTypeData() {
		exception.expect(ClientException.class);
		exception.expectMessage(ContentErrorMessageConstants.MISSING_CONTROLLER_FILES_ERROR);
		ECRFConversionUtility fixture = new ECRFConversionUtility();
		String strContent = getFileString("Verbs/index.ecml");
		Plugin plugin = fixture.getECRF(strContent);
		PipelineRequestorClient.getPipeline("missingCtrlValidatorProcessor", folder.getPath(), "test_12")
				.execute(plugin);
	}

	@Test
	public void duplicateController() {
		exception.expect(ClientException.class);
		exception.expectMessage(ContentErrorMessageConstants.DUPLICATE_CONTROLLER_ID_ERROR);
		ECRFConversionUtility fixture = new ECRFConversionUtility();
		String strContent = getFileString("/Sample_XML_1_ERROR_DUPLICATE_CONTROLLER.ecml");
		Plugin plugin = fixture.getECRF(strContent);
		PipelineRequestorClient
				.getPipeline("missingCtrlValidatorProcessor", folder.getPath(), "test_12")
				.execute(plugin);
	}
}