import {
  IonCard,
  IonCardContent,
  IonCardHeader,
  IonCardTitle,
  IonCol,
  IonContent,
  IonGrid,
  IonHeader,
  IonIcon,
  IonItem,
  IonLabel,
  IonList,
  IonNote,
  IonPage,
  IonRow,
  IonTitle,
  IonToolbar,
  useIonRouter,
  IonSpinner,
  IonButton,
  IonButtons,
  IonToast,
  IonSelect,
  IonSelectOption,
  IonSplitPane,
  IonMenu,
  IonMenuButton,
} from "@ionic/react";
import { 
  documentOutline, 
  checkmarkCircleOutline, 
  swapHorizontalOutline,
  gitCompareOutline,
} from "ionicons/icons";
import React, { useEffect, useState } from "react";
import Search from "../components/common/Search";
import ImportOscal from "../components/ImportOscal";
import { useOscal } from "../context/OscalContext";
import { StorageService } from "../services/storage";
import PackageSelector from "../components/PackageSelector";
import { ApiService, ConversionService } from "../services/api";
import { OscalPackage } from "../types";
import { RenderOscal } from "../components/oscal/RenderOscal";

interface DocumentEntry {
  id: string;
}

const Documents: React.FC = () => {
  const { packages, documents, setPackage, setDocumentId, documentId, packageId } = useOscal();
  const [menuEnabled, setMenuEnabled] = useState(true);
  const router = useIonRouter();
  const [documentList, setDocumentList] = useState<DocumentEntry[]>([]);
  const [selectedDocument, setSelectedDocument] = useState<Record<string, any> | null>(null);
  const [loading, setLoading] = useState(true);
  const [validating, setValidating] = useState(false);
  const [converting, setConverting] = useState(false);
  const [resolving, setResolving] = useState(false);
  const [toastMessage, setToastMessage] = useState<string>("");
  const [toastColor, setToastColor] = useState<string>("success");
  const [showToast, setShowToast] = useState(false);

  // Load document list when package changes
  useEffect(() => {
    const loadDocuments = async () => {
      setLoading(true);
      try {
        const docs = await documents();
        const entries: DocumentEntry[] = docs.map(filename => ({ id: filename }));
        setDocumentList(entries);
      } catch (error) {
        console.error('Failed to load documents:', error);
        setDocumentList([]);
      } finally {
        setLoading(false);
      }
    };

    loadDocuments();
  }, [documents, packageId]);

  // Load selected document content when documentId changes
  useEffect(() => {
    const loadSelectedDocument = async () => {
      if (!documentId || !packageId) {
        setSelectedDocument(null);
        return;
      }

      try {
        let content = await ApiService.getPackageFile(packageId, documentId);
        if (!documentId.endsWith("json")) {
          const jsonContent = JSON.parse(
            await ConversionService.convertFile(new File([content], documentId), "json")
          );
          setSelectedDocument(jsonContent);
        } else {
          setSelectedDocument(JSON.parse(content));
        }
      } catch (error) {
        console.error(`Failed to load document ${documentId}:`, error);
        setSelectedDocument(null);
      }
    };

    loadSelectedDocument();
  }, [documentId, packageId]);

  
  // Clear selected document when changing packages
  useEffect(() => {
    if (documentId && !documentList.find(entry => entry.id === documentId)) {
      setDocumentId('');
      router.push('/documents');
    }
  }, [documentList, documentId, setDocumentId, router]);

  // Get document ID from URL on initial load
  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const docId = params.get('id');
    if (docId != null && docId !== documentId) {
      setDocumentId(docId);
    }
  }, [setDocumentId, documentId]);


  const handleValidate = async () => {
    if (!selectedDocument || !documentId) return;
    
    setValidating(true);
    try {
      await ApiService.validatePackageDocument(packageId, documentId);
      setToastColor('success')
      setToastMessage("Document validation successful");
      setShowToast(true);
    } catch (error) {
      setToastColor('danger')
      setToastMessage(`Validation error: ${error instanceof Error ? error.message : 'Unknown error'}`);
      setShowToast(true);
    } finally {
      setValidating(false);
    }
  };

  const handleConvert = async (targetFormat:string) => {
    if (!selectedDocument || !documentId) return;
    
    setConverting(true);
    try {
      const convertedDocument = await ConversionService.convertPackageDocument(packageId, documentId,targetFormat);
      const url = window.URL.createObjectURL(new Blob([convertedDocument]));
      const a = document.createElement('a');
      a.href = url;
      a.download = `${documentId.split('.')[0]}.${targetFormat}`;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
      setToastMessage("Document converted successfully");
      setShowToast(true);
    } catch (error) {
      setToastMessage(`Conversion error: ${error instanceof Error ? error.message : 'Unknown error'}`);
      setShowToast(true);
    } finally {
      setConverting(false);
    }
  };

  const handleResolveProfile = async () => {
    if (!selectedDocument || !documentId) return;
    
    setResolving(true);
    try {
      const format = documentId.split('.').pop() || 'json';
      let content = await ApiService.getPackageFile(packageId, documentId);
      const resolved = await ApiService.resolveProfile(content, format);
      const blob = new Blob([resolved], { type: 'application/json' });
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `resolved-${documentId}`;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
      setToastMessage("Profile resolved successfully");
      setShowToast(true);
    } catch (error) {
      setToastMessage(`Profile resolution error: ${error instanceof Error ? error.message : 'Unknown error'}`);
      setShowToast(true);
    } finally {
      setResolving(false);
    }
  };

  return (
    <IonSplitPane contentId="documents-main">
      <IonMenu contentId="documents-main" side="start">
        <IonHeader>
          <IonToolbar>
            <IonTitle>Documents</IonTitle>
          </IonToolbar>
        </IonHeader>
        <IonContent>
          <ImportOscal 
            onImport={(documentId) => {
              router.push(`/documents?id=${documentId}`);
            }}
          />
          {loading ? (
            <IonItem>
              <IonLabel>
                <IonSpinner name="dots" /> Loading documents...
              </IonLabel>
            </IonItem>
          ) : (
            <IonList>
              {documentList.map(({id}) => (
                <IonItem 
                  key={id} 
                  button 
                  color={documentId === id ? "primary" : undefined}
                  onClick={() => {
                    setDocumentId(id);
                    router.push(`/documents?id=${id}`);
                  }}
                >
                  <IonIcon icon={documentOutline} slot="start" />
                  <IonLabel>
                    {id}
                  </IonLabel>
                </IonItem>
              ))}
              {documentList.length === 0 && (
                <IonItem>
                  <IonLabel color="medium">No documents available in package '{packageId}'. Use the import function above to add documents.</IonLabel>
                </IonItem>
              )}
            </IonList>
          )}
        </IonContent>
      </IonMenu>
      
      <IonPage id="documents-main">
        <IonToast
        isOpen={showToast}
        onDidDismiss={() => setShowToast(false)}
        message={toastMessage}
        color={toastColor}
        duration={3000}
        position="bottom"
      />
      <IonHeader>
        <IonToolbar>
          <IonButtons slot="start">
            <IonMenuButton />
          </IonButtons>
          <IonTitle>Documents</IonTitle>
          {selectedDocument && (
            <IonButtons slot="end">
              <IonButton onClick={handleValidate} disabled={validating}>
                <IonIcon slot="start" icon={checkmarkCircleOutline} />
                {validating ? 'Validating...' : 'Validate'}
              </IonButton>
              
              <IonSelect 
                value={""}
                onIonChange={e => {;
                   handleConvert(e.detail.value)}}
                interface="popover"
              >
                <IonSelectOption disabled value="">Convert</IonSelectOption>
                <IonSelectOption value="json">to JSON</IonSelectOption>
                <IonSelectOption value="yaml">to YAML</IonSelectOption>
                <IonSelectOption value="xml">to XML</IonSelectOption>
              </IonSelect>


              {/* {selectedDocument.profile && (
                <IonButton onClick={handleResolveProfile} disabled={resolving}>
                  <IonIcon slot="start" icon={gitCompareOutline} />
                  {resolving ? 'Resolving...' : 'Resolve Profile'}
                </IonButton>
              )} */}
            </IonButtons>
          )}
        </IonToolbar>
      </IonHeader>
      <IonContent>
        <IonGrid>
          <IonRow>
            <IonCol size="12">
              {documentId && selectedDocument ? (
                <IonCard>
                  <IonCardHeader>
                    <IonCardTitle>{packageId}-{documentId}</IonCardTitle>
                  </IonCardHeader>
                  <IonCardContent>
                    <div style={{ 
                      padding: '1rem',
                      backgroundColor: 'var(--ion-item-background)',
                      borderRadius: '4px',
                      margin: '0.5rem',
                      border: '1px solid var(--ion-border-color)',
                      boxShadow: '0 1px 2px var(--ion-color-step-100)'
                    }}>
                      {(()=>{console.log(selectedDocument);
                        return <></>
                      })()}
                    <RenderOscal document={selectedDocument as OscalPackage}/>
                    </div>
                  </IonCardContent>
                </IonCard>
              ) : (
                <IonCard style={{ 
                  display: 'flex', 
                  justifyContent: 'center', 
                  alignItems: 'center', 
                  height: 'calc(100% - 2rem)',
                  margin: '1rem'
                }}>
                  <IonCardContent style={{ textAlign: 'center' }}>
                    <IonIcon 
                      icon={documentOutline} 
                      style={{ 
                        fontSize: '48px', 
                        color: 'var(--ion-color-step-500)',
                        marginBottom: '1rem'
                      }} 
                    />
                    <p style={{ color: 'var(--ion-color-step-500)' }}>
                      Select a document from the list to view its contents
                    </p>
                  </IonCardContent>
                </IonCard>
              )}
            </IonCol>
          </IonRow>
        </IonGrid>
      </IonContent>
      </IonPage>
    </IonSplitPane>
  );
};

export default Documents;
