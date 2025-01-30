import React, { useCallback, useState, useEffect } from 'react';
import {
  IonContent,
  IonHeader,
  IonPage,
  IonTitle,
  IonToolbar,
  IonButton,
  IonIcon,
  IonToast,
  IonCard,
  IonCardContent,
  IonCardHeader,
  IonCardTitle,
  IonSelect,
  IonSelectOption,
  IonItem,
  IonLabel,
  IonList,
  IonListHeader,
  IonNote,
  useIonRouter,
} from '@ionic/react';
import { cloudUploadOutline, downloadOutline, documentOutline } from 'ionicons/icons';
import { useOscal } from '../context/OscalContext';
import ImportOscal from '../components/ImportOscal';
import { EntityType, OscalPackage, RootElementType } from '../types';
import { ConversionService } from '../services/api';

const ImportExport: React.FC = () => {
  const {  packages,documents, all ,insert,packageId} = useOscal();
  const router = useIonRouter();
  const [exportFormat, setExportFormat] = useState<'json' | 'xml' | 'yaml'>('json');
  const [showToast, setShowToast] = React.useState(false);
  const [toastMessage, setToastMessage] = React.useState('');
  const [documentData, setDocumentData] = React.useState({});
  const [toastColor, setToastColor] = React.useState<'success' | 'danger'>('success');

  const handleFileUpload = async (file: File) => {
    try {
      const jsonData = await ConversionService.convertFile(file, 'json');
      const documentType: RootElementType = Object.keys(jsonData)[0] as RootElementType;
      // Store the document as a package
      const docId = jsonData[documentType].metadata.uuid;
      await insert(documentType, jsonData);
      setDocumentData(jsonData);
      showNotification('OSCAL data imported successfully!', 'success');
      
      // Navigate to Documents page with the document ID
      router.push(`/documents?id=${docId}`);
    } catch (error) {
      showNotification(
        error instanceof Error ? error.message : 'Failed to import file',
        'danger'
      );
    }
  };

  const isValidOscalDocument = (data: any): boolean => {
    // Check if the document has at least one of the expected OSCAL root properties
    return !!(
      data.catalog ||
      data.profile ||
      data['component-definition'] ||
      data['system-security-plan'] ||
      data['assessment-plan'] ||
      data['assessment-results'] ||
      data['plan-of-action-and-milestones']
    );
  };

  const showNotification = (message: string, color: 'success' | 'danger') => {
    setToastMessage(message);
    setToastColor(color);
    setShowToast(true);
  };

  const handleExport = async () => {
    try {
      const blob = await ConversionService.exportFile(documentData, exportFormat);
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `oscal-document.${exportFormat}`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
      showNotification('OSCAL data exported successfully!', 'success');
    } catch (error) {
      showNotification(
        error instanceof Error ? error.message : 'Failed to export file',
        'danger'
      );
    }
  };

  const onDrop = useCallback((e: React.DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    const file = e.dataTransfer.files[0];
    if (file) {
      const fileExt = file.name.split('.').pop()?.toLowerCase();
      if (['json', 'xml', 'yaml', 'yml'].includes(fileExt || '')) {
        handleFileUpload(file);
      } else {
        showNotification('Please upload a JSON, XML, or YAML file', 'danger');
      }
    }
  }, []);

  const onDragOver = useCallback((e: React.DragEvent<HTMLDivElement>) => {
    e.preventDefault();
  }, []);

  return (
    <IonPage>
      <IonHeader>
        <IonToolbar>
          <IonTitle>Import/Export OSCAL</IonTitle>
        </IonToolbar>
      </IonHeader>
      <IonContent className="ion-padding">
        <IonCard>
          <IonCardHeader>
            <IonCardTitle>Import OSCAL Document</IonCardTitle>
          </IonCardHeader>
          <IonCardContent>
            <ImportOscal 
              onImport={(documentId) => {
                showNotification('OSCAL data imported successfully!', 'success');
                router.push('/documents?document='+documentId);
              }}
            />
          </IonCardContent>
        </IonCard>

        <IonCard>
          <IonCardHeader>
            <IonCardTitle>Export OSCAL Document</IonCardTitle>
          </IonCardHeader>
          <IonCardContent>
            <IonItem>
              <IonLabel>Export Format</IonLabel>
              <IonSelect 
                value={exportFormat} 
                onIonChange={e => setExportFormat(e.detail.value)}
              >
                <IonSelectOption value="json">JSON</IonSelectOption>
                <IonSelectOption value="xml">XML</IonSelectOption>
                <IonSelectOption value="yaml">YAML</IonSelectOption>
              </IonSelect>
            </IonItem>
            <IonButton
              expand="block"
              onClick={handleExport}
              disabled={!packageId}
            >
              <IonIcon slot="start" icon={downloadOutline} />
              Export {exportFormat.toUpperCase()}
            </IonButton>
          </IonCardContent>
        </IonCard>

        <IonCard>
          <IonCardHeader>
            <IonCardTitle>Imported Documents</IonCardTitle>
          </IonCardHeader>
          <IonCardContent>
            <IonList>
              <IonListHeader>
                <IonLabel>Recent Imports</IonLabel>
              </IonListHeader>
              {Object.entries(documents()||{}).map(([id, doc]) => (
                <IonItem 
                  key={id} 
                  button 
                  onClick={() => router.push(`/documents?id=${id}`)}
                >
                  <IonIcon icon={documentOutline} slot="start" />
                  <IonLabel>
                    {(doc as any)?.metadata?.title || 'Untitled Document'}
                    <IonNote slot="helper">
                      {new Date(parseInt(id.split('-')[1])).toLocaleDateString()}
                    </IonNote>
                  </IonLabel>
                </IonItem>
              ))}
              {Object.keys(documentData).length === 0 && (
                <IonItem>
                  <IonLabel color="medium">no document loaded</IonLabel>
                </IonItem>
              )}
            </IonList>
          </IonCardContent>
        </IonCard>

        <IonToast
          isOpen={showToast}
          onDidDismiss={() => setShowToast(false)}
          message={toastMessage}
          duration={3000}
          color={toastColor}
          position="bottom"
        />
      </IonContent>
    </IonPage>
  );
};

export default ImportExport;
