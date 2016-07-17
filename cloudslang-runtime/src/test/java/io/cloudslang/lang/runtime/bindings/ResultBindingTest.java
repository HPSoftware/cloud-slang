package io.cloudslang.lang.runtime.bindings;
/*******************************************************************************
* (c) Copyright 2014 Hewlett-Packard Development Company, L.P.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Apache License v2.0 which accompany this distribution.
*
* The Apache License is available at
* http://www.apache.org/licenses/LICENSE-2.0
*
*******************************************************************************/


import io.cloudslang.dependency.api.services.DependencyService;
import io.cloudslang.dependency.api.services.MavenConfig;
import io.cloudslang.dependency.impl.services.DependencyServiceImpl;
import io.cloudslang.dependency.impl.services.MavenConfigImpl;
import io.cloudslang.lang.entities.ScoreLangConstants;
import io.cloudslang.lang.entities.SystemProperty;
import io.cloudslang.lang.entities.bindings.Result;
import io.cloudslang.lang.entities.bindings.values.Value;
import io.cloudslang.lang.entities.bindings.values.ValueFactory;
import io.cloudslang.lang.runtime.bindings.scripts.ScriptEvaluator;
import io.cloudslang.pypi.*;
import io.cloudslang.pypi.transformers.PackageTransformer;
import io.cloudslang.pypi.transformers.TarballPackageTransformer;
import io.cloudslang.pypi.transformers.WheelPackageTransformer;
import io.cloudslang.runtime.api.python.PythonRuntimeService;
import io.cloudslang.runtime.impl.python.PythonExecutionCachedEngine;
import io.cloudslang.runtime.impl.python.PythonExecutionEngine;
import io.cloudslang.runtime.impl.python.PythonRuntimeServiceImpl;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.Serializable;
import java.util.*;

/**
 * User: stoneo
 * Date: 06/11/2014
 * Time: 10:02
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = ResultBindingTest.Config.class)
public class ResultBindingTest {

    @SuppressWarnings("unchecked")
    private static final Set<SystemProperty> EMPTY_SET = Collections.EMPTY_SET;
    
    @Autowired
    private ResultsBinding resultsBinding;

    @Test
    public void testPrimitiveBooleanFirstResult() throws Exception {
        List<Result> results = Arrays.asList(
                createResult(ScoreLangConstants.SUCCESS_RESULT, ValueFactory.create(true)),
                createResult(ScoreLangConstants.FAILURE_RESULT, ValueFactory.create("${ True and (not False) }"))
        );
        String result = resultsBinding.resolveResult(new HashMap<String, Value>(), new HashMap<String, Value>(), EMPTY_SET, results, null);
        Assert.assertEquals(ScoreLangConstants.SUCCESS_RESULT, result);
    }

    @Test
    public void testPrimitiveBooleanSecondResult() throws Exception {
        List<Result> results = Arrays.asList(
                createResult(ScoreLangConstants.SUCCESS_RESULT, ValueFactory.create(false)),
                createResult(ScoreLangConstants.FAILURE_RESULT, ValueFactory.create(true))
        );
        String result = resultsBinding.resolveResult(new HashMap<String, Value>(), new HashMap<String, Value>(), EMPTY_SET, results, null);
        Assert.assertEquals(ScoreLangConstants.FAILURE_RESULT, result);
    }

    @Test
    public void testObjectBooleanFirstResult() throws Exception {
        List<Result> results = Arrays.asList(
                createResult(ScoreLangConstants.SUCCESS_RESULT, ValueFactory.create(Boolean.TRUE)),
                createResult(ScoreLangConstants.FAILURE_RESULT, ValueFactory.create("${ True and (not False) }"))
        );
        String result = resultsBinding.resolveResult(new HashMap<String, Value>(), new HashMap<String, Value>(), EMPTY_SET, results, null);
        Assert.assertEquals(ScoreLangConstants.SUCCESS_RESULT, result);
    }

    @Test
    public void testObjectBooleanSecondResult() throws Exception {
        List<Result> results = Arrays.asList(
                createResult(ScoreLangConstants.SUCCESS_RESULT, ValueFactory.create(Boolean.FALSE)),
                createResult(ScoreLangConstants.FAILURE_RESULT, ValueFactory.create(Boolean.TRUE))
        );
        String result = resultsBinding.resolveResult(new HashMap<String, Value>(), new HashMap<String, Value>(), EMPTY_SET, results, null);
        Assert.assertEquals(ScoreLangConstants.FAILURE_RESULT, result);
    }

    @Test
    public void testConstExprChooseFirstResult() throws Exception {
        List<Result> results = Arrays.asList(createResult(ScoreLangConstants.SUCCESS_RESULT, ValueFactory.create("${ 1==1 }")),
                createResult(ScoreLangConstants.FAILURE_RESULT, ValueFactory.create("${ True and (not False) }")));
        String result = resultsBinding.resolveResult(new HashMap<String, Value>(), new HashMap<String, Value>(), EMPTY_SET, results, null);
        Assert.assertEquals(ScoreLangConstants.SUCCESS_RESULT, result);
    }

    @Test
    public void testConstExprChooseSecondAResult() throws Exception {
        List<Result> results = Arrays.asList(createResult(ScoreLangConstants.SUCCESS_RESULT, ValueFactory.create("${ 1==2 }")),
                                                createResult(ScoreLangConstants.FAILURE_RESULT, ValueFactory.create("${ 1==1 }")));
        String result = resultsBinding.resolveResult(new HashMap<String, Value>(), new HashMap<String, Value>(), EMPTY_SET, results, null);
        Assert.assertEquals(ScoreLangConstants.FAILURE_RESULT, result);
    }

    @Test
    public void testBindInputFirstResult() throws Exception {
        List<Result> results = Arrays.asList(createResult(ScoreLangConstants.SUCCESS_RESULT, ValueFactory.create("${ int(status) == 1 }")),
                createResult(ScoreLangConstants.FAILURE_RESULT, ValueFactory.create("${ int(status) == -1 }")));
        HashMap<String, Value> context = new HashMap<>();
        context.put("status", ValueFactory.create("1"));
        String result = resultsBinding.resolveResult(new HashMap<String, Value>(), context, EMPTY_SET, results, null);
        Assert.assertEquals(ScoreLangConstants.SUCCESS_RESULT, result);
    }

    @Test
    public void testBindInputSecondResult() throws Exception {
        List<Result> results = Arrays.asList(createResult(ScoreLangConstants.SUCCESS_RESULT, ValueFactory.create("${ int(status) == 1 }")),
                                                createResult(ScoreLangConstants.FAILURE_RESULT, ValueFactory.create("${ int(status) == -1 }")));
        HashMap<String, Value> context = new HashMap<>();
        context.put("status", ValueFactory.create("-1"));
        String result = resultsBinding.resolveResult(new HashMap<String, Value>(), context, EMPTY_SET, results, null);
        Assert.assertEquals(ScoreLangConstants.FAILURE_RESULT, result);
    }

    @Test(expected = RuntimeException.class)
    public void testIllegalResultExpressionThrowsException() throws Exception {
        List<Result> results = Arrays.asList(createResult(ScoreLangConstants.SUCCESS_RESULT, ValueFactory.create("${ str(status) }")),
                                                createResult(ScoreLangConstants.FAILURE_RESULT, ValueFactory.create("${ int(status) == -1 }")));
        HashMap<String, Value> context = new HashMap<>();
        context.put("status", ValueFactory.create("-1"));
        resultsBinding.resolveResult(new HashMap<String, Value>(), context, EMPTY_SET, results, null);
    }

    @Test
    public void testBindInputNullResult() throws Exception {
        List<Result> results = Arrays.asList(createResult(ScoreLangConstants.SUCCESS_RESULT, ValueFactory.create("${ int(status) == 1 }")),
                                                createResult(ScoreLangConstants.FAILURE_RESULT, null));
        HashMap<String, Value> context = new HashMap<>();
        context.put("status", ValueFactory.create("-1"));
        String result = resultsBinding.resolveResult(new HashMap<String, Value>(), context, EMPTY_SET, results, null);
        Assert.assertEquals(ScoreLangConstants.FAILURE_RESULT, result);
    }

    @Test(expected = RuntimeException.class)
    public void testNoResults() throws Exception {
        List<Result> results = Collections.emptyList();
        HashMap<String, Value> context = new HashMap<>();
        context.put("status", ValueFactory.create("-1"));
        resultsBinding.resolveResult(new HashMap<String, Value>(), context, EMPTY_SET, results, null);
    }

    @Test(expected = RuntimeException.class)
    public void testNoValidResultExpression() throws Exception {
        List<Result> results = Arrays.asList(createResult(ScoreLangConstants.SUCCESS_RESULT, ValueFactory.create("${ int(status) == 1 }")),
                createResult(ScoreLangConstants.FAILURE_RESULT, ValueFactory.create("${ int(status) == 0 }")));
        HashMap<String, Value> context = new HashMap<>();
        context.put("status", ValueFactory.create("-1"));
        resultsBinding.resolveResult(new HashMap<String, Value>(), context, EMPTY_SET, results, null);
    }

    @Test
    public void testPresetResult() throws Exception {
        List<Result> results = Arrays.asList(createEmptyResult(ScoreLangConstants.SUCCESS_RESULT),
                createEmptyResult(ScoreLangConstants.FAILURE_RESULT));
        HashMap<String, Value> context = new HashMap<>();
        String result = resultsBinding.resolveResult(new HashMap<String, Value>(), context, EMPTY_SET, results, ScoreLangConstants.FAILURE_RESULT);
        Assert.assertEquals(ScoreLangConstants.FAILURE_RESULT, result);
    }

    @Test(expected = RuntimeException.class)
    public void testIllegalPresetResult() throws Exception {
        List<Result> results = Arrays.asList(createEmptyResult(ScoreLangConstants.SUCCESS_RESULT),
                createEmptyResult(ScoreLangConstants.FAILURE_RESULT));
        HashMap<String, Value> context = new HashMap<>();
        resultsBinding.resolveResult(new HashMap<String, Value>(), context, EMPTY_SET, results, "IllegalResult");
    }

    @Test(expected = RuntimeException.class)
    public void testIllegalResultExpression() throws Exception {
        List<Result> results = Arrays.asList(
                createResult(ScoreLangConstants.SUCCESS_RESULT, ValueFactory.create("${ status }")),
                createResult(ScoreLangConstants.FAILURE_RESULT, ValueFactory.create((Serializable)null)));
        HashMap<String, Value> context = new HashMap<>();
        context.put("status", ValueFactory.create("-1"));
        resultsBinding.resolveResult(new HashMap<String, Value>(), context, EMPTY_SET, results, null);
    }

    private Result createResult(String name, Value expression){
        return new Result(name, expression);
    }

    private Result createEmptyResult(String name){
        return new Result(name, null);
    }

    @Configuration
    static class Config{

        @Bean
        public ResultsBinding resultsBinding(){
            return new ResultsBinding();
        }

        @Bean
        public ScriptEvaluator scriptEvaluator(){
            return new ScriptEvaluator();
        }

        @Bean
        public DependencyService mavenRepositoryService() {
            return new DependencyServiceImpl();
        }

        @Bean
        public MavenConfig mavenConfig() {
            return new MavenConfigImpl();
        }

        @Bean
        public PythonRuntimeService pythonRuntimeService(){
            return new PythonRuntimeServiceImpl();
        }

        @Bean
        public PythonExecutionEngine pythonExecutionEngine(){
            return new PythonExecutionCachedEngine();
        }

        @Bean
        public Pip2MavenAdapter pip2MavenAdapter() {
            return new Pip2MavenAdapterImpl();
        }

        @Bean
        public Pip2MavenTransformer pip2MavenTransformer() {
            return new Pip2MavenTransformerImpl();
        }

        @Bean
        public Pip pip() {
            return new PipImpl();
        }

        @Bean
        public PackageTransformer wheelPackageTransformer() {
            return new WheelPackageTransformer();
        }

        @Bean
        public PackageTransformer tarballPackageTransformer() {
            return new TarballPackageTransformer();
        }

        @Bean
        public Pip2MavenTransformer pip2Maven() {
            return new Pip2MavenTransformerImpl();
        }
    }
}
