package org.nd4j.linalg.api.ops.impl.layers.convolution;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import onnx.OnnxProto3;
import org.nd4j.autodiff.functions.DifferentialFunction;
import org.nd4j.autodiff.samediff.SDVariable;
import org.nd4j.autodiff.samediff.SameDiff;
import org.nd4j.imports.NoOpNameFoundException;
import org.nd4j.imports.converters.DifferentialFunctionClassHolder;
import org.nd4j.imports.descriptors.properties.AttributeAdapter;
import org.nd4j.imports.descriptors.properties.PropertyMapping;
import org.nd4j.imports.descriptors.properties.adapters.ConditionalFieldValueIntIndexArrayAdapter;
import org.nd4j.imports.descriptors.properties.adapters.ConditionalFieldValueNDArrayShapeAdapter;
import org.nd4j.imports.descriptors.properties.adapters.SizeThresholdIntArrayIntIndexAdpater;
import org.nd4j.imports.descriptors.properties.adapters.StringEqualsAdapter;
import org.nd4j.imports.graphmapper.onnx.OnnxGraphMapper;
import org.nd4j.imports.graphmapper.tf.TFGraphMapper;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.api.ops.DynamicCustomOp;
import org.nd4j.linalg.api.ops.impl.layers.convolution.config.Conv2DConfig;
import org.nd4j.linalg.util.ArrayUtil;
import org.tensorflow.framework.AttrValue;
import org.tensorflow.framework.GraphDef;
import org.tensorflow.framework.NodeDef;

import java.lang.reflect.Field;
import java.util.*;


/**
 * Depthwise Conv2D operation
 */
@Slf4j
@Getter
public class DepthwiseConv2D extends DynamicCustomOp {

    protected Conv2DConfig config;

    @Builder(builderMethodName = "builder")
    public DepthwiseConv2D(SameDiff sameDiff,
                           SDVariable[] inputFunctions,
                           INDArray[] inputArrays, INDArray[] outputs,
                           Conv2DConfig config) {
        super(null, inputArrays, outputs);
        this.sameDiff = sameDiff;
        this.config = config;
        addArgs();
        sameDiff.putFunctionForId(this.getOwnName(), this);    //Normally called in DynamicCustomOp constructor, via setInstanceId - but sameDiff field is null at that point
        sameDiff.addArgsFor(inputFunctions, this);
    }

    public DepthwiseConv2D() {
    }

    protected void addArgs() {
        addIArgument(new long[]{config.getKh(),
                config.getKw(),
                config.getSy(),
                config.getSx(),
                config.getPh(),
                config.getPw(),
                config.getDh(),
                config.getDw(),
                ArrayUtil.fromBoolean(config.isSameMode()),
                ArrayUtil.fromBoolean(config.isNHWC())});

    }

    @Override
    public Object getValue(Field property) {
        if (config == null) {
            config = Conv2DConfig.builder().build();
        }

        try {
            val t = config.getValue(property);
            return t;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setValueFor(Field target, Object value) {
        config.setValueFor(target, value);
    }

    @Override
    public Map<String, Object> propertiesForFunction() {
        return config.toProperties();
    }

    @Override
    public void initFromTensorFlow(NodeDef nodeDef, SameDiff initWith, Map<String, AttrValue> attributesForNode, GraphDef graph) {
        TFGraphMapper.getInstance().initFunctionFromProperties(nodeDef.getOp(), this, attributesForNode, nodeDef, graph);
        addArgs();

        /*
        // we must permute weights once during import
        val weightsName = nodeDef.getInput(1);
        val variable = initWith.getVariable(weightsName);
        val tmp = initWith.getArrForVarName(weightsName);
        val array = tmp.permute(3, 2, 0, 1).dup('c');

        initWith.associateArrayWithVariable(array, variable);
        */
    }

    @Override
    public boolean isConfigProperties() {
        return true;
    }

    @Override
    public String configFieldName() {
        return "config";
    }

    @Override
    public void initFromOnnx(OnnxProto3.NodeProto node, SameDiff initWith, Map<String, OnnxProto3.AttributeProto> attributesForNode, OnnxProto3.GraphProto graph) {
        OnnxGraphMapper.getInstance().initFunctionFromProperties(node.getOpType(), this, attributesForNode, node, graph);
        addArgs();
    }


    @Override
    public Map<String, Map<String, AttributeAdapter>> attributeAdaptersForFunction() {
        Map<String, Map<String, AttributeAdapter>> ret = new HashMap<>();
        Map<String, AttributeAdapter> tfMappings = new LinkedHashMap<>();
        val fields = DifferentialFunctionClassHolder.getInstance().getFieldsForFunction(this);


        tfMappings.put("kh", new ConditionalFieldValueNDArrayShapeAdapter("NCHW", 0, 0, fields.get("dataFormat")));
        tfMappings.put("kw", new ConditionalFieldValueNDArrayShapeAdapter("NCHW", 1, 1, fields.get("dataFormat")));
        tfMappings.put("sy", new ConditionalFieldValueIntIndexArrayAdapter("NCHW", 2, 1, fields.get("dataFormat")));
        tfMappings.put("sx", new ConditionalFieldValueIntIndexArrayAdapter("NCHW", 3, 2, fields.get("dataFormat")));
        tfMappings.put("isSameMode", new StringEqualsAdapter("SAME"));
        tfMappings.put("isNHWC", new StringEqualsAdapter("NHWC"));


        Map<String, AttributeAdapter> onnxMappings = new HashMap<>();
        onnxMappings.put("kh", new SizeThresholdIntArrayIntIndexAdpater(0, 2, 0));
        onnxMappings.put("kw", new SizeThresholdIntArrayIntIndexAdpater(1, 2, 0));
        onnxMappings.put("dh", new SizeThresholdIntArrayIntIndexAdpater(0, 2, 0));
        onnxMappings.put("dw", new SizeThresholdIntArrayIntIndexAdpater(1, 2, 0));
        onnxMappings.put("sy", new SizeThresholdIntArrayIntIndexAdpater(0, 2, 0));
        onnxMappings.put("sx", new SizeThresholdIntArrayIntIndexAdpater(1, 2, 0));
        onnxMappings.put("isSameMode", new StringEqualsAdapter("SAME"));
        onnxMappings.put("isNHWC", new StringEqualsAdapter("NHWC"));


        try {
            ret.put(tensorflowName(), tfMappings);
        } catch (NoOpNameFoundException e) {
            //
        }

        try {
            ret.put(onnxName(), onnxMappings);
        } catch (NoOpNameFoundException e) {
            //
        }

        return ret;
    }

    @Override
    public Map<String, Map<String, PropertyMapping>> mappingsForFunction() {
        Map<String, Map<String, PropertyMapping>> ret = new HashMap<>();
        Map<String, PropertyMapping> map = new HashMap<>();
        val strideMapping = PropertyMapping.builder()
                .tfAttrName("strides")
                .onnxAttrName("strides")
                .propertyNames(new String[]{"sx", "sy"})
                .build();


        val kernelMappingH = PropertyMapping.builder()
                .propertyNames(new String[]{"kh"})
                .tfInputPosition(1)
                .shapePosition(0)
                .onnxAttrName("kernel_shape")
                .build();

        val kernelMappingW = PropertyMapping.builder()
                .propertyNames(new String[]{"kw"})
                .tfInputPosition(1)
                .shapePosition(1)
                .onnxAttrName("kernel_shape")
                .build();

        val dilationMapping = PropertyMapping.builder()
                .onnxAttrName("dilations")
                .propertyNames(new String[]{"dw", "dh"})
                .tfAttrName("rates")
                .build();

        val dataFormat = PropertyMapping.builder()
                .onnxAttrName("data_format")
                .tfAttrName("data_format")
                .propertyNames(new String[]{"dataFormat"})
                .build();

        val nhwc = PropertyMapping.builder()
                .onnxAttrName("data_format")
                .tfAttrName("data_format")
                .propertyNames(new String[]{"isNHWC"})
                .build();

        val sameMode = PropertyMapping.builder()
                .onnxAttrName("auto_pad")
                .propertyNames(new String[]{"isSameMode"})
                .tfAttrName("padding")
                .build();

        val paddingWidthHeight = PropertyMapping.builder()
                .onnxAttrName("padding")
                .propertyNames(new String[]{"ph", "pw"})
                .build();


        map.put("sx", strideMapping);
        map.put("sy", strideMapping);
        map.put("kh", kernelMappingH);
        map.put("kw", kernelMappingW);
        map.put("dw", dilationMapping);
        map.put("dh", dilationMapping);
        map.put("isSameMode", sameMode);
        map.put("ph", paddingWidthHeight);
        map.put("pw", paddingWidthHeight);
        map.put("dataFormat", dataFormat);
        map.put("isNHWC", nhwc);

        try {
            ret.put(onnxName(), map);
        } catch (NoOpNameFoundException e) {
            //ignore
        }


        try {
            ret.put(tensorflowName(), map);
        } catch (NoOpNameFoundException e) {
            //ignore
        }

        return ret;
    }


    @Override
    public String opName() {
        return "depthwise_conv2d";
    }

    @Override
    public List<SDVariable> doDiff(List<SDVariable> f1) {
        throw new UnsupportedOperationException("Not implemented yet");
    }


    @Override
    public String onnxName() {
        return "depth_conv";
    }

    @Override
    public String tensorflowName() {
        return "DepthwiseConv2dNative";
    }
}
