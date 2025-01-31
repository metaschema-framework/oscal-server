import React from 'react';
import { IonAccordion, IonItem, IonLabel, IonList, IonChip, IonNote } from '@ionic/react';
import { BackMatter, Resource } from '../../types';
import { RenderProps } from './RenderProps';

interface RenderBackMatterProps {
  backMatter: BackMatter;
}

const openResource = (href: string, mediaType?: string) => {
  window.open(href, '_blank');
};

export const RenderBackMatter: React.FC<RenderBackMatterProps> = ({ backMatter }) => {
  if (!backMatter?.resources?.length) return null;

  return (
    <IonAccordion value="resources">
      <IonItem slot="header" color="light">
        <IonLabel>Resources</IonLabel>
      </IonItem>
      <div className="ion-padding" slot="content">
        <IonList>
          {backMatter.resources.map((resource: Resource, index: number) => (
            <IonItem key={resource.uuid || index}>
              <IonLabel>
                <h2>{resource.title}</h2>
                {resource.description && <p>{resource.description}</p>}
                {resource.props && <RenderProps props={resource.props} />}
                <div style={{ display: 'flex', gap: '8px', marginTop: '8px' }}>
                  {resource.rlinks?.map((rlink, rlinkIndex) => (
                    <div key={rlinkIndex} style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                      {rlink['media-type'] && (
                        <IonChip 
                          color="tertiary" 
                          onClick={() => openResource(rlink.href, rlink['media-type'])}
                        >
                          {rlink['media-type'].split('/')[1] || rlink['media-type']}
                        </IonChip>
                      )}
                    </div>
                  ))}
                </div>
                {resource.citation && (
                  <IonNote className="ion-margin-top">
                    <p><strong>Citation:</strong> {resource.citation.text}</p>
                  </IonNote>
                )}
              </IonLabel>
            </IonItem>
          ))}
        </IonList>
      </div>
    </IonAccordion>
  );
};
