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
} from "@ionic/react";
import { documentOutline } from "ionicons/icons";
import React, { useEffect, useState } from "react";
import RenderOscal from '../components/RenderOscal';
import Search from "../components/common/Search";
import ImportOscal from "../components/ImportOscal";
import { useOscal } from "../context/OscalContext";
import { StorageService } from "../services/storage";
import PackageSelector from "../components/PackageSelector";
import { ApiService, ConversionService } from "../services/api";

interface DocumentEntry {
  id: string;
}

const Documents: React.FC = () => {
  const { packages, documents, setPackage, setDocumentId, documentId, packageId } = useOscal();
  const router = useIonRouter();
  const [documentList, setDocumentList] = useState<DocumentEntry[]>([]);
  const [selectedDocument, setSelectedDocument] = useState<Record<string, any> | null>(null);
  const [loading, setLoading] = useState(true);

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

  const getDocumentTitle = (doc: Record<string, any>) => {
    // Try to determine title from OSCAL content first
    if (doc.catalog?.metadata?.title) return doc.catalog.metadata.title;
    if (doc.profile?.metadata?.title) return doc.profile.metadata.title;
    if (doc['component-definition']?.metadata?.title) return doc['component-definition'].metadata.title;
    if (doc['system-security-plan']?.metadata?.title) return doc['system-security-plan'].metadata.title;
    if (doc['assessment-plan']?.metadata?.title) return doc['assessment-plan'].metadata.title;
    if (doc['assessment-results']?.metadata?.title) return doc['assessment-results'].metadata.title;
    if (doc['plan-of-action-and-milestones']?.metadata?.title) return doc['plan-of-action-and-milestones'].metadata.title;
    
    // Fallback to direct title if available
    if (doc.catalog?.title) return doc.catalog.title;
    if (doc.profile?.title) return doc.profile.title;
    if (doc['component-definition']?.title) return doc['component-definition'].title;
    if (doc['system-security-plan']?.title) return doc['system-security-plan'].title;
    if (doc['assessment-plan']?.title) return doc['assessment-plan'].title;
    if (doc['assessment-results']?.title) return doc['assessment-results'].title;
    if (doc['plan-of-action-and-milestones']?.title) return doc['plan-of-action-and-milestones'].title;
    
    // Last fallback
    if (doc.metadata?.title) return doc.metadata.title;
    
    return 'Untitled Document';
  };

  const getDocumentType = (doc: Record<string, any>) => {
    if (doc.catalog) return 'Catalog';
    if (doc.profile) return 'Profile';
    if (doc['component-definition']) return 'Component Definition';
    if (doc['system-security-plan']) return 'System Security Plan';
    if (doc['assessment-plan']) return 'Assessment Plan';
    if (doc['assessment-results']) return 'Assessment Results';
    if (doc['plan-of-action-and-milestones']) return 'Plan of Action and Milestones';
    return 'Unknown Type';
  };


  return (
    <IonPage>
      <IonHeader>
        <IonToolbar>
          <IonTitle>Documents</IonTitle>
        </IonToolbar>
      </IonHeader>
      <IonContent>
        <IonGrid>
          <IonRow>
            {/* Left Column - Document List */}
            <IonCol size="12" sizeMd="4" style={{ borderRight: '1px solid var(--ion-border-color)' }}>
              {/* <Search context="Documents" /> */}
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
            </IonCol>

            {/* Right Column - Document Content */}
            <IonCol size="12" sizeMd="8">
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
                    <RenderOscal document={selectedDocument}/>
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
  );
};

export default Documents;
