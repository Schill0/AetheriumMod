import onnx

model = onnx.load(r"c:\Users\UTN\Desktop\EspMod\src\main\resources\assets\espmod\ai\radar_donut_v1.onnx")
print("Input info:")
for input in model.graph.input:
    print(input.name, input.type)
print("\nOutput info:")
for output in model.graph.output:
    print(output.name, output.type)
