import React from 'react';
import { IonAccordion, IonItem, IonLabel, IonList } from '@ionic/react';
import { DocumentMetadata } from '../../types';
import { RenderProps } from './RenderProps';

interface RenderMetadataProps {
  metadata: DocumentMetadata;
}

export const RenderMetadata: React.FC<RenderMetadataProps> = ({ metadata }) => {
  if (!metadata) return null;

  return (
    <IonAccordion value="metadata">
      <IonItem slot="header" color="light">
        <IonLabel>Metadata</IonLabel>
      </IonItem>
      <div className="ion-padding" slot="content">
        <IonList>
          {metadata.title && (
            <IonItem>
              <IonLabel>
                <h2>Title</h2>
                <p>{metadata.title}</p>
              </IonLabel>
            </IonItem>
          )}
          {metadata.version && (
            <IonItem>
              <IonLabel>
                <h2>Version</h2>
                <p>{metadata.version}</p>
              </IonLabel>
            </IonItem>
          )}
          {metadata['oscal-version'] && (
            <IonItem>
              <IonLabel>
                <h2>OSCAL Version</h2>
                <p>{metadata['oscal-version']}</p>
              </IonLabel>
            </IonItem>
          )}
          {metadata.props && <RenderProps props={metadata.props} />}
        </IonList>
      </div>
    </IonAccordion>
  );
};
