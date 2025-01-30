import React, { useState, useRef, KeyboardEvent, useEffect } from "react";
import {
  IonButton,
  IonFooter,
  IonHeader,
  IonIcon,
  IonInput,
  IonItem,
  IonList,
  IonModal,
  IonToolbar,
  IonContent,
  IonTitle,
  IonSpinner,
  IonText,
  IonChip,
  IonAlert,
} from "@ionic/react";
import {
  sendSharp,
  chatbubbleEllipsesSharp,
  closeCircleOutline,
  alertCircleOutline,
  closeOutline,
} from "ionicons/icons";
import { useChatbot } from "../../context/ChatbotContext";
import { useOscal } from "../../context/OscalContext";
import { OscalPackage } from "../../types";
import {
  generateOllamaResponse,
  useOllamaStatus,
  downloadGraniteModelIfNeeded,
} from "../../services/ollamaService";
import {
  validateDocument,
  checkValidationService,
} from "../../services/sarifService";

const ChatbotWindow: React.FC = () => {
  const { messages, addMessage, clearMessages } = useChatbot();
  const { } = useOscal();
  const [showModal, setShowModal] = useState(false);
  const [inputText, setInputText] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [showAlert, setShowAlert] = useState(false);
  const [alertMessage, setAlertMessage] = useState("");
  const [validationAvailable, setValidationAvailable] =
    useState<boolean>(false);
  const contentRef = useRef<HTMLIonContentElement>(null);
  const inputRef = useRef<HTMLIonInputElement>(null);
  const modelStatus = useOllamaStatus();

  useEffect(() => {
    if (showModal) {
      // Check model status
      if (modelStatus.status !== "ready") {
        downloadGraniteModelIfNeeded().catch((error) => {
          console.error("Failed to download model:", error);
          setAlertMessage(
            "Failed to initialize AI model. Some features may be limited."
          );
          setShowAlert(true);
        });
      }

      // Check validation service
      checkValidationService().then((isAvailable: boolean) => {
        setValidationAvailable(isAvailable);
        if (!isAvailable) {
          console.warn("OSCAL validation service is not available");
        }
      });
    }
  }, [showModal, modelStatus.status]);

  const generateOscalSuggestion = (
    input: string,
    data: OscalPackage | null
  ): string => {
    if (!data) {
      return "Please import or create an OSCAL document first. I can help you with that process.";
    }

    const lowercaseInput = input.toLowerCase();

    // Document type-specific suggestions
    if ("catalog" in data) {
      if (
        lowercaseInput.includes("control") ||
        lowercaseInput.includes("requirement")
      ) {
        return "I notice you're working with a catalog. I can help you manage controls and their properties. Would you like to:\n1. Add a new control\n2. Modify existing controls\n3. Review control dependencies";
      }
    } else if ("profile" in data) {
      if (
        lowercaseInput.includes("import") ||
        lowercaseInput.includes("customize")
      ) {
        return "For your profile document, I can help you:\n1. Import controls from a catalog\n2. Customize control parameters\n3. Set up control baselines";
      }
    } else if ("system-security-plan" in data) {
      if (
        lowercaseInput.includes("system") ||
        lowercaseInput.includes("security")
      ) {
        return "I see you're working on a System Security Plan. I can help you with:\n1. System characteristics\n2. Security control implementation\n3. Authorization boundary";
      }
    } else if ("assessment-plan" in data) {
      if (
        lowercaseInput.includes("assess") ||
        lowercaseInput.includes("plan")
      ) {
        return "For your Assessment Plan, I can assist with:\n1. Assessment objectives\n2. Assessment activities\n3. Assessment methods";
      }
    } else if ("assessment-results" in data) {
      if (
        lowercaseInput.includes("result") ||
        lowercaseInput.includes("finding")
      ) {
        return "I can help analyze your Assessment Results:\n1. Review findings\n2. Analyze observations\n3. Track risk responses";
      }
    } else if ("plan-of-action-and-milestones" in data) {
      if (
        lowercaseInput.includes("action") ||
        lowercaseInput.includes("milestone")
      ) {
        return "For your Plan of Action and Milestones, I can help:\n1. Track findings\n2. Manage remediation plans\n3. Monitor milestones";
      }
    } else if ("component-definition" in data) {
      if (
        lowercaseInput.includes("component") ||
        lowercaseInput.includes("definition")
      ) {
        return "For your Component Definition, I can assist with:\n1. Component properties\n2. Control implementations\n3. Component relationships";
      }
    }

    // General suggestions based on common keywords
    if (lowercaseInput.includes("validate")) {
      return "I can help validate your OSCAL document. Would you like me to check for:\n1. Required fields\n2. Property format compliance\n3. Reference integrity";
    } else if (
      lowercaseInput.includes("metadata") ||
      lowercaseInput.includes("info")
    ) {
      return "I can help you manage document metadata. Would you like to:\n1. Update revision history\n2. Add responsible parties\n3. Manage document properties";
    } else if (
      lowercaseInput.includes("export") ||
      lowercaseInput.includes("save")
    ) {
      return "I can help you export your OSCAL document. Would you like to:\n1. Export as JSON\n2. Validate before export\n3. Review export options";
    }

    // Default response
    return "I can help you with your OSCAL document. You can ask about:\n1. Document validation\n2. Metadata management\n3. Control customization\n4. Import/Export options";
  };

  const scrollToBottom = () => {
    if (contentRef.current) {
      contentRef.current.scrollToBottom(300);
    }
  };


  const formatTimestamp = (timestamp?: number) => {
    if (!timestamp) return "";
    return new Date(timestamp).toLocaleTimeString([], {
      hour: "2-digit",
      minute: "2-digit",
    });
  };

  return (
    <>
      <IonButton
        fill="clear"
        onClick={() => setShowModal(true)}
        style={{
          position: "fixed",
          top: "5px",
          right: "5px",
          zIndex: 999,
        }}
      >
        <IonIcon slot="icon-only" icon={chatbubbleEllipsesSharp} />
      </IonButton>

      <IonModal
        isOpen={showModal}
        onDidDismiss={() => setShowModal(false)}
        breakpoints={[0, 1]}
        initialBreakpoint={1}
        style={{ "--height": "500px" }}
      >
        <IonHeader>
          <IonToolbar>
            <IonTitle>OSCAL Assistant</IonTitle>
            <IonButton
              fill="clear"
              slot="end"
              onClick={() => setShowModal(false)}
            >
              <IonIcon color='danger' slot="icon-only" icon={closeOutline} />
            </IonButton>
          </IonToolbar>
        </IonHeader>

        <IonContent ref={contentRef}>
          <IonList>
            {messages.map((m, i) => (
              <IonItem
                key={i}
                style={{
                  "--background":
                    m.role === "bot"
                      ? "var(--ion-color-light)"
                      : m.role === "system"
                      ? "var(--ion-color-warning-tint)"
                      : "transparent",
                  marginBottom: "8px",
                }}
              >
                <div style={{ width: "100%" }}>
                  <IonChip
                    color={m.role === "bot" ? "primary" : "secondary"}
                    style={{ margin: "0 0 4px 0" }}
                  >
                    {m.role === "bot" ? "🤖 Assistant" : "👤 You"}
                    <IonText style={{ marginLeft: "4px", fontSize: "0.8em" }}>
                      {formatTimestamp(m.timestamp)}
                    </IonText>
                  </IonChip>
                  <div style={{ whiteSpace: "pre-wrap", padding: "4px 0" }}>
                    {m.text}
                  </div>
                </div>
              </IonItem>
            ))}
            {isLoading && (
              <IonItem>
                <IonSpinner name="dots" />
                <IonText color="medium" style={{ marginLeft: "8px" }}>
                  Assistant is thinking...
                </IonText>
              </IonItem>
            )}
          </IonList>
        </IonContent>

        <IonFooter>
          <IonToolbar>
            <IonItem lines="none">
              <IonInput
                ref={inputRef}
                value={inputText}
                placeholder="Type a message..."
                onIonChange={(e) => setInputText(e.detail.value || "")}
                clearInput
                style={{ "--padding-start": "0" }}
              />
              <IonButton
                slot="end"
                disabled={!inputText.trim() || isLoading}
                fill='clear'
              >
                <IonIcon  slot="icon-only" icon={sendSharp} />
              </IonButton>
            </IonItem>
          </IonToolbar>
        </IonFooter>
      </IonModal>
      <IonAlert
        isOpen={showAlert}
        onDidDismiss={() => setShowAlert(false)}
        header="Notice"
        message={alertMessage}
        buttons={["OK"]}
      />
    </>
  );
};

export default ChatbotWindow;
