package ee.jakarta.tck.concurrent.framework;

import static ee.jakarta.tck.concurrent.common.TestGroups.JAKARTAEE_FULL_PROPERTY;
import static ee.jakarta.tck.concurrent.common.TestGroups.JAKARTAEE_FULL;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.testng.IMethodInstance;
import org.testng.IMethodInterceptor;
import org.testng.ITestContext;

public class FullProfileInterceptor implements IMethodInterceptor {
	
	private static final boolean isFullProfile = Boolean.getBoolean(JAKARTAEE_FULL_PROPERTY);

	@Override
	public List<IMethodInstance> intercept(List<IMethodInstance> methods, ITestContext context) {
		System.out.println("KJA1017 jakarta.run.full.profile=" + isFullProfile);
		
		if(isFullProfile) {
			//We should run all test methods, return original list
			return methods;
		} else {
			//Search through all methods and exclude those that are part of the JAKARTAEE_FULL group.
	        List<IMethodInstance> newList = new ArrayList<>();
	        for(IMethodInstance m : methods) {
	        	List<String> groups = Arrays.asList(m.getMethod().getGroups());
	        	if(groups.contains(JAKARTAEE_FULL)) {
	        		//Do not add
	        	} else {
	        		newList.add(m);
	        	}
	        }
	        return newList;
		}
	}
}
