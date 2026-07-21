package com.miofeather.commandblockstudio.main.insight;

import java.util.List;
import java.util.Map;

public record CommandDoc(String name, String summary, List<String> examples, Map<String, String> nodes, Map<String, String> arguments) {
    public String describeNode(String nodeName) {
        return nodes.get(nodeName);
    }

    public String describeArgument(String argumentName) {
        return arguments.get(argumentName);
    }
}
