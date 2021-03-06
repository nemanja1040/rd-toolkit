package org.rdtoolkit.component

import android.content.Context
import org.rdtoolkit.component.capture.WindowCaptureComponentManifest
import org.rdtoolkit.component.processing.MockClassifierManifest

class ComponentRepository(context: Context) {

    val imageCaptureManifests = HashSet<ToolkitComponentManifest<TestImageCaptureComponent, Any>>()

    val classifierManifests = HashSet<ToolkitComponentManifest<ImageClassifierComponent, Any>>()

    fun registerCaptureComponent(manifest: ToolkitComponentManifest<TestImageCaptureComponent, *>){
        imageCaptureManifests.add(manifest as ToolkitComponentManifest<TestImageCaptureComponent, Any>)
    }

    fun registerClassifierComponent(manifest: ToolkitComponentManifest<ImageClassifierComponent, *>){
        classifierManifests.add(manifest as ToolkitComponentManifest<ImageClassifierComponent, Any>)
    }

    fun getCaptureComponentForTest(testProfileId: String, tags: MutableSet<String>, compatibleCaptureFormats : List<String>?, sandbox: Sandbox) : TestImageCaptureComponent {
        val matchingComponents =
                imageCaptureManifests.filter { it.getTagsForDiagnostic(testProfileId).containsAll(tags) }
                        .filter { compatibleCaptureFormats == null || it.getCompatibleOutputs(testProfileId).intersect(compatibleCaptureFormats).isNotEmpty() }
                        .sortedByDescending { it.getValue() }
        if (matchingComponents.isEmpty()) {
            throw Exception("No matching Capture Components available!")
        }
        val component = matchingComponents.first()
        return component.getComponent(component.getConfigForDiagnostic(testProfileId), sandbox)
    }

    fun getClassifierComponentForTest(testProfileId: String, tags : Set<String>, sandbox: Sandbox) : ImageClassifierComponent? {
        val matchingComponents = classifierManifests.filter { it.getTagsForDiagnostic(testProfileId).containsAll(tags) }.sortedByDescending { it.getValue() }
        if (matchingComponents.isEmpty()) {
            return null
        }
        val component = matchingComponents.first()
        return component.getComponent(component.getConfigForDiagnostic(testProfileId), sandbox)
    }

    init {
        registerCaptureComponent(PlainCameraComponentManifest())
        registerCaptureComponent(WindowCaptureComponentManifest())
        registerClassifierComponent(MockClassifierManifest())

        //NOTE: I don't love this because it's pretty one-to-one
        StaticComponentRegistry(context).bootstrap(this)
    }
}