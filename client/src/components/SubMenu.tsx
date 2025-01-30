import {
  IonTab,
  IonTabBar,
  IonTabButton,
  IonTabs
} from '@ionic/react';
import { useHistory, useLocation, Switch, Route } from 'react-router-dom';

export interface SubRoute {
  slug: string;
  component: React.FC;
  description?: string;
  title?: string;
}

interface SubMenuProps {
  routes: SubRoute[];
}

const SubMenu: React.FC<SubMenuProps> = ({ routes }) => {
  const history = useHistory();
  const location = useLocation();
  const pathSegments = location.pathname.split("/").filter(segment => segment);
  const base = pathSegments[0];
  const currentSlug = pathSegments[1] || 'index';
  
  // Handle tab click and update URL only if changing routes
  const handleTabClick = (slug: string) => {
    // Don't push to history if we're already on the target route
    if (currentSlug === slug) return;
    
    if (slug === 'index') {
      history.push(`/${base}`);
    } else {
      history.push(`/${base}/${slug}`);
    }
  };

  // Sort routes so that 'index' is first, then alphabetically
  const sortedRoutes = [...routes].sort((a, b) => {
    if (a.slug === 'index') return -1;
    if (b.slug === 'index') return 1;
    return a.slug.localeCompare(b.slug);
  });

  return (
    <IonTabs>
      <IonTab tab="content">
        <Switch>
          {sortedRoutes.map((route) => (
            <Route
              key={route.slug}
              path={`/${base}${route.slug === 'index' ? '' : '/' + route.slug}`}
              exact
            >
              <route.component />
            </Route>
          ))}
        </Switch>
      </IonTab>
      <IonTabBar slot="bottom">
        {sortedRoutes.map((route) => (
          <IonTabButton 
            key={route.slug} 
            tab={route.slug}
            selected={currentSlug === route.slug}
            onClick={() => handleTabClick(route.slug)}
          >
            {route.slug === 'index' ? 'Home' : route.slug}
          </IonTabButton>
        ))}
      </IonTabBar>
    </IonTabs>
  );
};
export default SubMenu;
