import { Route, Routes } from 'react-router-dom'
import Layout from './components/Layout'
import CampaignDetailPage from './pages/CampaignDetailPage'
import CampaignsPage from './pages/CampaignsPage'
import CreateCampaignPage from './pages/CreateCampaignPage'

export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<CampaignsPage />} />
        <Route path="campaigns/new" element={<CreateCampaignPage />} />
        <Route path="campaigns/:id" element={<CampaignDetailPage />} />
      </Route>
    </Routes>
  )
}
