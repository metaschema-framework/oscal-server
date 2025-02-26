import React from 'react';
import { IonAccordion, IonItem, IonLabel } from '@ionic/react';
import { ModifyControls } from '../../types';
import { RenderParts } from './RenderParts';
import { RenderProps } from './RenderProps';

interface RenderModifyProps {
  modify: ModifyControls;
}

export const RenderModify: React.FC<RenderModifyProps> = ({ modify }) => {
  return (
    <IonAccordion value="modify">
      <IonItem slot="header" color="light">
        <IonLabel>Modify Controls</IonLabel>
      </IonItem>
      <div className="ion-padding" slot="content">
        {modify['set-parameters'] && modify['set-parameters'].length > 0 && (
          <div className="parameters-container">
            <h4>Parameter Settings</h4>
            <div className="parameters-grid">
              {modify['set-parameters']?.map((param, idx) => (
                <div key={`param-${idx}`} className="parameter-card">
                  <div className="parameter-header">
                    <span className="parameter-id">{param['param-id']}</span>
                    {param.label && <span className="parameter-label">{param.label}</span>}
                  </div>
                  
                  {param.values && param.values.length > 0 && (
                    <div className="parameter-values">
                      <h6>Values:</h6>
                      <div className="value-chips">
                        {param.values?.map((value, vidx) => (
                          <span key={`value-${vidx}`} className="value-chip">
                            {value}
                          </span>
                        ))}
                      </div>
                    </div>
                  )}
                  {JSON.stringify(param)}
                    {param.constraints && param.constraints.length > 0 && (
                    <div className="parameter-constraints">
                      <h6>Constraints:</h6>
                      <div className="value-chips">
                        {param.constraints?.map((value, vidx) => (
                          <span key={`value-${vidx}`} className="value-chip">
                          {value.description}
                        </span>
                      ))}
                      </div>
                    </div>
                  )}
                  
                  {param.usage && (
                    <div className="parameter-usage">
                      <h6>Usage:</h6>
                      <p>{param.usage}</p>
                    </div>
                  )}
                  
                  {param.props && (
                    <div className="parameter-props">
                      <h6>Properties:</h6>
                      <RenderProps props={param.props} />
                    </div>
                  )}
                </div>
              ))}
            </div>
          </div>
        )}

        {modify.alters && modify.alters.length > 0 && (
          <div>
            <h4>Control Alterations</h4>
            {modify.alters?.map((alter, idx) => (
              <div key={`alter-${idx}`}>
                <h5>Control ID: {alter['control-id']}</h5>
                
                {alter.removes && alter.removes.length > 0 && (
                  <div>
                    <h6>Removals</h6>
                    {alter.removes?.map((remove, ridx) => (
                      <div key={`remove-${ridx}`}>
                        {remove['by-name'] && <p>By Name: {remove['by-name']}</p>}
                        {remove['by-class'] && <p>By Class: {remove['by-class']}</p>}
                        {remove['by-id'] && <p>By ID: {remove['by-id']}</p>}
                        {remove['by-item-name'] && <p>By Item Name: {remove['by-item-name']}</p>}
                        {remove['by-ns'] && <p>By Namespace: {remove['by-ns']}</p>}
                      </div>
                    ))}
                  </div>
                )}

                {alter.adds && alter.adds.length > 0 && (
                  <div>
                    <h6>Additions</h6>
                    {alter.adds?.map((add, aidx) => (
                      <div key={`add-${aidx}`}>
                        {add.position && <p>Position: {add.position}</p>}
                        {add['by-id'] && <p>By ID: {add['by-id']}</p>}
                        {add.title && <p>Title: {add.title}</p>}
                        {add.props && <RenderProps props={add.props} />}
                        {add.parts && <RenderParts parts={add.parts} />}
                      </div>
                    ))}
                  </div>
                )}
              </div>
            ))}
          </div>
        )}
      </div>
    </IonAccordion>
  );
};
