import {
  IonButtons,
  IonCard,
  IonCardContent,
  IonCardHeader,
  IonCardTitle,
  IonChip,
  IonIcon,
  IonLabel,
  IonText
} from "@ionic/react";
import { documentOutline } from "ionicons/icons";
import React, { useState } from "react";
import { useOscal } from "../context/OscalContext";
import { DocumentMetadata, OscalPackage, ProvidedUUID, RootElementType } from "../types";
import PackageSelector from "./PackageSelector";
import { ConversionService } from "../services/api";

interface ImportOscalProps {
  type?: string;
  onImport: (documentId:string) => void;
}

interface OscalPackageWithMetadata extends OscalPackage {
  uuid: string;
  metadata: DocumentMetadata;
}

const ImportOscal: React.FC<ImportOscalProps> = ({
  onImport,
}) => {
  const [file, setFile] = useState<File | null>(null);
  const [error, setError] = useState("");

  const { insert,saveDocument, all, packages, packageId: selectedPackage ,setDocumentId} = useOscal();
  const documentTypes = [
    { value: "catalog", label: "Catalog" },
    { value: "profile", label: "Profile" },
    { value: "component-definition", label: "Component Definition" },
    { value: "system-security-plan", label: "System Security Plan" },
    { value: "assessment-plan", label: "Assessment Plan" },
    { value: "assessment-results", label: "Assessment Results" },
    {
      value: "plan-of-action-and-milestones",
      label: "Plan of Action and Milestones",
    },
  ] as const;

  const handleFileUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files?.[0]) {
      const file = e.target.files[0];

      try {
        if (!selectedPackage) {
          throw new Error("Please select a package");
        }

        const fileExt = file.name.split(".").pop()?.toLowerCase();

        if (!["json","xml", "yaml", "yml"].includes(fileExt || "")) {
          throw new Error(
            "Unsupported file format. Please upload a JSON, XML, or YAML file."
          );
        }

        
        try {
          setDocumentId(file.name);
          console.log(file);
          await saveDocument(file);
          console.log("Package saved successfully");
          onImport(file.name);
        } catch (err) {
          console.error("Failed to save package:", err);
          console.error(
            "Package data that failed:",
            JSON.stringify(file, null, 2)
          );
          throw new Error(
            "Failed to save package. Please check console for details."
          );
        }

        setFile(null);
        setError("");
      } catch (err) {
        setError(err instanceof Error ? err.message : "Failed to process file");
      }
    }
  };

  return (
    <IonCard>
      <IonCardHeader>
        <IonCardTitle>Import Document</IonCardTitle>
      </IonCardHeader>
      <IonCardContent>
        <IonLabel position="stacked">Package</IonLabel>
        <IonChip color='tertiary'>
        {selectedPackage}
        </IonChip>
        <div
          onDragOver={(e) => {
            e.preventDefault();
            e.currentTarget.style.backgroundColor = "var(--ion-color-light)";
          }}
          onDragLeave={(e) => {
            e.preventDefault();
            e.currentTarget.style.backgroundColor = "transparent";
          }}
          onDrop={(e) => {
            e.preventDefault();
            e.currentTarget.style.backgroundColor = "transparent";
            if (e.dataTransfer.files?.[0]) {
              handleFileUpload({
                target: { files: e.dataTransfer.files },
              } as any);
            }
          }}
          style={{
            border: "2px dashed var(--ion-color-medium)",
            borderRadius: "8px",
            padding: "20px",
            textAlign: "center",
            cursor: "pointer",
            marginBottom: "16px",
            marginTop: "16px",
            transition: "background-color 0.3s ease",
          }}
          onClick={() => document.getElementById("file-input")?.click()}
        >
          <input
            id={"file-input"}
            type="file"
            accept=".json,.xml,.yaml,.yml"
            onChange={handleFileUpload}
            style={{ display: "none" }}
          />
          <IonIcon
            icon={documentOutline}
            style={{
              fontSize: "48px",
              color: "var(--ion-color-medium)",
              marginBottom: "8px",
            }}
          />
          <div style={{ color: "var(--ion-color-medium)" }}>
            <p style={{ margin: "4px 0" }}>Drag and drop your file here</p>
            <p style={{ margin: "4px 0", fontSize: "0.9em" }}>
              or click to browse
            </p>
            <p style={{ margin: "4px 0", fontSize: "0.8em", opacity: 0.8 }}>
              Supports JSON, XML, and YAML files
            </p>
          </div>
        </div>

        {error && (
          <IonText color="danger">
            <p>{error}</p>
          </IonText>
        )}
      </IonCardContent>
    </IonCard>
  );
};

export default ImportOscal;
